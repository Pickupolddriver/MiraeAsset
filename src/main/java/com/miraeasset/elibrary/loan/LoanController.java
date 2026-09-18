package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.loan.dto.BorrowBookRequest;
import com.miraeasset.elibrary.loan.dto.LoanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan created"),
            @ApiResponse(responseCode = "400", description = "Invalid request or user identity",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Book unavailable or active loan already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LoanResponse> borrow(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody BorrowBookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrow(userId, request));
    }

    @GetMapping("/current")
    @Operation(summary = "List the current user's active loans")
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Invalid or missing user identity",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))))
    public List<LoanResponse> currentLoans(@RequestHeader("X-User-Id") String userId) {
        return loanService.currentLoans(userId);
    }

    @PutMapping("/{loanId}/return")
    @Operation(summary = "Return a loan")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid request or user identity",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Loan is owned by another user",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Loan or book not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public LoanResponse returnLoan(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID loanId) {
        return loanService.returnLoan(userId, loanId);
    }
}
