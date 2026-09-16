#!/usr/bin/env bash
#
# Lightweight performance baseline for the E-Library take-home.
#
# Expects the application to already be running (e.g. `mvn spring-boot:run`)
# with bulk-seeded data. It measures HTTP request latency from a pure curl
# client perspective:
#
#   GET /api/books?page=0&size=20      browse first page
#   GET /api/books?q=clean...          keyword search
#   GET /api/books?page=200&size=20    deep pagination
#   GET /api/books/{id}                book detail
#   POST /api/loans ... then return    borrow + return round trip (locks a row)
#   GET /api/admin/loans/current       admin current-loans view
#
# Usage:
#   BASE=http://localhost:8080 REPEATS=30 ./scripts/benchmark.sh
#   BASE=http://localhost:8080 REPEATS=30 SKIP_BORROW=1 ./scripts/benchmark.sh  # read-only
#
set -euo pipefail

BASE="${BASE:-http://localhost:8080}"
N="${REPEATS:-50}"
USER_ID="bench-user"
ADMIN_ID="bench-admin"

# JSON field extraction without jq: greedy capture of `"id":<number>`
extract_id() {
  grep -oE '"id":[0-9]+' | head -1 | grep -oE '[0-9]+'
}

echo "Server: $BASE"
echo "Repeats: $N"
echo

MKTMP="$(mktemp -d -t bench.XXXXXX)"
trap 'rm -rf "$MKTMP"' EXIT

# Print summary stats (seconds) from a file holding one curl time_total per line.
report() {
  local label="$1"
  local file="$2"
  printf '%-52s\n' "== $label =="
  printf 'count=%d  min=%.4f  avg=%.4f  med=%.4f  p95=%.4f  max=%.4f\n' \
    "$(wc -l < "$file" | tr -d ' ')" \
    "$(sort -n "$file" | head -1)" \
    "$(awk '{s+=$1} END{printf "%.4f", s/NR}' "$file")" \
    "$(sort -n "$file" | awk -v c="$(wc -l < "$file" | tr -d ' ')" 'NR==int((c+1)/2){print; exit}')" \
    "$(sort -n "$file" | awk -v c="$(wc -l < "$file" | tr -d ' ')" 'NR==int(c*0.95<1?1:c*0.95){print; exit}')" \
    "$(sort -n "$file" | tail -1)"
  echo
}

# Collect N samples of a curl call, appending time_total to a temp file.
measure() {
  local label="$1"
  shift
  local file="$MKTMP/$(echo "$label" | tr -cd 'A-Za-z0-9' | head -c 20).times"
  for _ in $(seq 1 "$N"); do
    curl -s -o /dev/null -w '%{time_total}\n' "$@" >> "$file"
  done
  report "$label" "$file"
}

measure "GET /api/books?page=0&size=20" "$BASE/api/books" -G --data-urlencode "page=0" --data-urlencode "size=20"
measure "GET /api/books?q=<search>&size=20" "$BASE/api/books" -G --data-urlencode "q=clean" --data-urlencode "size=20"
measure "GET /api/books?page=200&size=20" "$BASE/api/books" -G --data-urlencode "page=200" --data-urlencode "size=20"

BOOK_ID="$(curl -s "$BASE/api/books?page=0&size=20" | extract_id)"
echo "Using bookId=$BOOK_ID for detail / borrow tests."
echo
if [ -z "$BOOK_ID" ]; then
  echo "WARNING: could not resolve a book id; skipping detail and borrow tests."
else
  measure "GET /api/books/{bookId} (detail)" "$BASE/api/books/$BOOK_ID"
fi

if [ -n "${SKIP_BORROW:-}" ]; then
  echo "SKIP_BORROW set; skipping borrow/return (write path)."
else
  if [ -z "$BOOK_ID" ]; then
    echo "  (detail/borrow skipped: no book id)"
  else
    # Measure a real borrow -> return round trip per iteration so the row lock
    # (PESSIMISTIC_WRITE) is exercised repeatedly without a duplicate-loan 409.
    bt="$MKTMP/borrow.times"
    rt="$MKTMP/return.times"
    b_body="$MKTMP/borrow.body"
    for _ in $(seq 1 "$N"); do
      # one borrow; body to file, time_total to stdout append.
      curl -s -o "$b_body" -w '%{time_total}\n' \
        -X POST -H "X-User-Id: $USER_ID" -H 'Content-Type: application/json' \
        -d "{\"bookId\":$BOOK_ID}" "$BASE/api/loans" >> "$bt"
      LOAN_ID="$(grep -oE '"loanId":"[^"]+"' "$b_body" | head -1 | grep -oE '[0-9a-fA-F-]{36}' || true)"
      if [ -n "$LOAN_ID" ]; then
        curl -s -o /dev/null -w '%{time_total}\n' \
          -X PUT -H "X-User-Id: $USER_ID" "$BASE/api/loans/$LOAN_ID/return" >> "$rt"
      else
        echo '0.0' >> "$rt"
      fi
    done
    report "POST /api/loans (borrow, PESSIMISTIC_WRITE)" "$bt"
    report "PUT /api/loans/{id}/return (return, PESSIMISTIC_WRITE)" "$rt"
  fi
fi

measure "GET /api/admin/loans/current (admin)" \
  "$BASE/api/admin/loans/current" -H "X-User-Id: $ADMIN_ID" -H "X-User-Role: ADMIN"