package com.miraeasset.elibrary.config;

import com.miraeasset.elibrary.common.ForbiddenOperationException;
import com.miraeasset.elibrary.identity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Centralized, default-deny authorization for the admin namespace. Every request
 * under {@code /api/admin/**} is rejected unless it carries the ADMIN role claim,
 * so a future admin handler is protected by default instead of relying on each
 * controller remembering to guard itself. The same {@code X-User-Role} header
 * contracts that {@link PrincipalArgumentResolver} already validates.
 */
@Component
public class AdminRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String role = request.getHeader("X-User-Role");
        if (!Role.ADMIN.name().equalsIgnoreCase(role)) {
            throw new ForbiddenOperationException("ADMIN_ROLE_REQUIRED", "This endpoint requires the ADMIN role");
        }
        return true;
    }
}