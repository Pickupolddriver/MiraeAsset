package com.miraeasset.elibrary.identity;

/**
 * The authenticated identity carried by a request. For the take-home this is
 * resolved from the {@code X-User-Id} / {@code X-User-Role} headers; a real
 * deployment replaces those headers with an authenticated principal (e.g. OIDC)
 * while keeping this same seam.
 *
 * @param userId the caller's identifier
 * @param role   the caller's role, used to guard the management surface
 */
public record Principal(String userId, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}