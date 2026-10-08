package com.yep.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.yep.exception.UnauthorizedException;

/** Small facade over the security context used by services and controllers. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser currentOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser principal)) {
            return null;
        }
        return principal;
    }

    public static AuthenticatedUser current() {
        AuthenticatedUser principal = currentOrNull();
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public static Long currentUserId() {
        return current().id();
    }
}
