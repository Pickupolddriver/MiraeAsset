package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.loan.dto.BorrowBookRequest;
import com.miraeasset.elibrary.loan.dto.LoanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loans")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @Operation(summary = "Borrow a book")
    public ResponseEntity<LoanResponse> borrow(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody BorrowBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrow(userId, request));
    }

    @GetMapping("/current")
    @Operation(summary = "List the current user's active loans")
    public List<LoanResponse> currentLoans(@RequestHeader("X-User-Id") String userId) {
        return loanService.currentLoans(userId);
    }

    @PutMapping("/{loanId}/return")
    @Operation(summary = "Return a loan")
    public LoanResponse returnLoan(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID loanId) {
        return loanService.returnLoan(userId, loanId);
    }
}
