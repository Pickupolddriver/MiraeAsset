package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.common.ForbiddenOperationException;
import com.miraeasset.elibrary.identity.Principal;
import com.miraeasset.elibrary.loan.dto.LoanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/loans")
@RequiredArgsConstructor
@Tag(name = "Admin Loans")
public class AdminController {

    private final LoanService loanService;

    @GetMapping("/current")
    @Operation(summary = "List all active loans across every user (admin only)")
    public List<LoanResponse> currentLoans(Principal principal) {
        if (!principal.isAdmin()) {
            throw new ForbiddenOperationException("ADMIN_ROLE_REQUIRED", "This endpoint requires the ADMIN role");
        }
        return loanService.allCurrentLoans();
    }
}