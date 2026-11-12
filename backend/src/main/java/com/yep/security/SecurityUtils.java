package com.yep.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.yep.exception.UnauthorizedException;

@Component
public class SecurityUtils {

    public AuthenticatedUser currentUser() {
        AuthenticatedUser user = currentUserOrNull();
        if (user == null) {
            throw new UnauthorizedException("AUTHENTICATION_REQUIRED", "Please sign in to continue.");
        }
        return user;
    }

    /** Returns null for anonymous requests (public endpoints); no exception. */
    public AuthenticatedUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    public Long currentUserId() {
        return currentUser().userId();
    }
}
