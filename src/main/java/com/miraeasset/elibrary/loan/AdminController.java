package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.common.dto.PageResponse;
import com.miraeasset.elibrary.identity.Principal;
import com.miraeasset.elibrary.loan.dto.LoanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/loans")
@RequiredArgsConstructor
@Tag(name = "Admin Loans")
public class AdminController {

    private final LoanService loanService;

    @GetMapping("/current")
    @Operation(summary = "List all active loans across every user (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid pagination",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role is required",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<LoanResponse> currentLoans(Principal principal,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return loanService.allCurrentLoans(page, size);
    }
}
