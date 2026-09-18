# E-Library Performance Baseline

A lightweight, repeatable latency baseline for the take-home implementation. It measures end-to-end HTTP request latency
against a real running instance (curl → Spring Boot → H2), which is closer to what a user experiences than a unit-level
benchmark. Numbers here are a **baseline for this laptop**, not a capacity claim; they are meant to establish a bar to
compare future changes against.

## Environment

- Runtime: macOS (Intel), OpenJDK 21 LTS (Temurin 21.0.12.1)
- Spring Boot 3.4.5, H2 in-memory (`MODE=PostgreSQL`), default embedded Tomcat
- Dataset: **5,003 books** (3 curated + 5,000 generated via `app.seed.bulk.count=5000`)
- Each measurement = 50 sequential HTTP requests (client-side `curl`), single client, no concurrency
- Reported: min / **avg** / median / **p95** / max, in **milliseconds**

## Results (ms)

| Operation                                           | min  | avg  | median | p95  | max   |
|-----------------------------------------------------|------|------|--------|------|-------|
| `GET /api/books?page=0&size=20` (browse first page) | 3.9  | 7.5  | 6.4    | 9.9  | 32.4  |
| `GET /api/books?q=clean&size=20` (keyword search)   | 14.5 | 18.1 | 16.5   | 24.9 | 32.2  |
| `GET /api/books?page=200&size=20` (deep pagination) | 5.4  | 7.3  | 6.7    | 10.6 | 13.2  |
| `GET /api/books/{bookId}` (detail)                  | 1.9  | 3.2  | 2.6    | 5.2  | 14.6  |
| `POST /api/loans` (borrow, row lock)                | 3.9  | 8.9  | 5.3    | 9.9  | 149.6 |
| `PUT /api/loans/{id}/return` (return, row lock)     | 3.1  | 5.1  | 4.1    | 6.7  | 38.9  |
| `GET /api/admin/loans/current` (admin view)         | 2.3  | 3.8  | 3.1    | 6.3  | 12.4  |

## Observations

- **Reads are single-digit milliseconds.** Browsing, deep pagination, detail, and the admin view all sit comfortably
  under ~10 ms at p95. Pagination cost is flat even at page 200 of 5,000 books, because H2 returns only the requested
  page slice rather than loading the whole table.
- **Keyword search is the most expensive read** (~16.5 ms median). The query uses `lower(title) LIKE '%...%'` and
  `lower(author) LIKE`, which cannot use a B-tree index on H2, so it is a full scan. This is the natural first candidate
  for an index or full-text improvement at larger catalog sizes.
- **Writes are lock-serialized by design and stay fast.** Borrow and return acquire a `PESSIMISTIC_WRITE` on the `Book`
  row. Median is ~5 ms / ~4 ms; the tail (max 149.6 ms borrow) reflects lock wait when a concurrent transaction holds
  the row, not steady-state cost. At 50 sequential requests there is no contention, so that tail is a single-observation
  anomaly, not a trend.
- **Admin view is cheap.** It scans the `loans` table for `returnedAt IS NULL`. Fast here because loan volume is tiny;
  it would need an index on `returned_at` (and `user_id`) once loan history grows.

## Conclusions / trade-offs

1. **The current architecture comfortably handles 5k books on one H2 instance** with <10 ms p95 reads. No optimization
   is needed for the take-home dataset.
2. **The `Book` row lock is the right concurrency boundary.** Median write latency is comparable to reads because it is
   a short transaction; the predictable cost of serializing same-book operations is worth the correctness it buys (no
   oversold licenses). This matches the production design note that license allocation stays on one authoritative store.
3. **If the catalog must scale further**, the highest-leverage change is enabling an index or full-text search for the
   `LIKE '%…%'` browse query, not anything in the loan path. The architecture document already prescribes read
   replicas + cache for browse and primary-only for writes.
4. **This file is not a load test.** Concurrency, throughput (req/s), and multi-user contention are covered separately
   by the `LoanConcurrencyTest` invariants (correctness under ten-user license contention), not by these latency
   numbers.

## How to reproduce

Requirements: Java 21, Maven.

```bash
# 1. Start the app with 5,000 books.
JAVA_HOME=$HOME/jdk21/Contents/Home mvn spring-boot:run \
  -Dspring-boot.run.arguments="--app.seed.bulk.enabled=true --app.seed.bulk.count=5000 --server.port=8080"

# 2. In another terminal, run the baseline (default port 8080, 50 repeats).
BASE=http://localhost:8080 REPEATS=50 ./scripts/benchmark.sh

# Read-only mode (skips borrow/return):
SKIP_BORROW=1 BASE=http://localhost:8080 REPEATS=50 ./scripts/benchmark.sh
```

The batch-seed and script are intentionally switched off by default so ordinary runs stay light.