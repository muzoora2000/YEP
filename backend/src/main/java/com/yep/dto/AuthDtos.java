package com.yep.dto;

import java.time.OffsetDateTime;
import java.util.Set;

import com.yep.domain.enums.Role;
import com.yep.domain.user.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Authentication and registration contracts for {@code /api/v1/auth}.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "full name is required")
            @Size(max = 120)
            String fullName,

            @NotBlank(message = "email is required")
            @Email(message = "email must be valid")
            @Size(max = 190)
            String email,

            @NotBlank(message = "password is required")
            @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
            String password,

            @Size(max = 80)
            String country,

            @Size(max = 30)
            String phone,

            @NotNull(message = "role is required")
            Role role,

            @NotNull(message = "consent is required")
            Boolean consentGiven,

            // participant only
            Integer age,
            com.yep.domain.enums.SkillLevel skillLevel,

            // facilitator only
            @Size(max = 120)
            String expertiseArea,
            @Size(max = 400)
            String expertiseTopics,

            // mentor only
            @Size(max = 400)
            String mentorshipAreas) {
    }

    public record LoginRequest(
            @NotBlank(message = "email is required")
            @Email
            String email,

            @NotBlank(message = "password is required")
            String password) {
    }

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresInSeconds,
            UserDtos.UserResponse user) {

        public static AuthResponse bearer(String token, long expiresInSeconds, UserDtos.UserResponse user) {
            return new AuthResponse(token, "Bearer", expiresInSeconds, user);
        }
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }

    /** Small helper shared by the mapper: masks nothing, but exposes no PII beyond what the owner needs. */
    public record SessionInfo(Long userId, String fullName, String email, Role role, Set<String> permissions) {
        public static SessionInfo from(User user) {
            return new SessionInfo(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                    user instanceof com.yep.domain.user.Admin admin ? admin.permissionSet() : Set.of());
        }
    }

    public record AuditTrail(String action, OffsetDateTime at) {
    }
}
