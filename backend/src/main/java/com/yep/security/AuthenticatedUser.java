package com.yep.security;

import java.security.Principal;

import com.yep.domain.enums.Role;

/**
 * The authenticated principal stored in the Spring Security context.
 * A compact, serialization-free value object (no JPA entities in the security
 * context - keeps the backend stateless and safe for horizontal scaling).
 */
public record AuthenticatedUser(Long id, String email, String fullName, Role role) implements Principal {

    @Override
    public String getName() {
        return email;
    }

    public boolean hasRole(Role required) {
        return role == required;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStaff() {
        return role == Role.FACILITATOR || role == Role.MENTOR || role == Role.ADMIN;
    }
}
