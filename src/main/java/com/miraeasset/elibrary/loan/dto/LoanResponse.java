package com.miraeasset.elibrary.loan.dto;

import com.miraeasset.elibrary.loan.Loan;

import java.time.Instant;
import java.util.UUID;

public record LoanResponse(
        UUID loanId,
        Long bookId,
        String bookTitle,
        String userId,
        String status,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt) {

    public static LoanResponse from(Loan loan) {
        return new LoanResponse(loan.getId(), loan.getBook().getId(), loan.getBook().getTitle(), loan.getUserId(),
                loan.isActive() ? "ACTIVE" : "RETURNED", loan.getBorrowedAt(), loan.getDueAt(), loan.getReturnedAt());
    }
}
