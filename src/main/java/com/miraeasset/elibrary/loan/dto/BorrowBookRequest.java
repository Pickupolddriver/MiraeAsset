package com.miraeasset.elibrary.loan.dto;

import jakarta.validation.constraints.NotNull;

public record BorrowBookRequest(@NotNull Long bookId) {
}
