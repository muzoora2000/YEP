package com.yep.dto;

import java.time.OffsetDateTime;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.OrganisationType;
import com.yep.domain.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$", message = "Password must be 8-100 characters and include a letter and a number.") String password,
            @NotNull Role role,
            @Size(max = 30) String phone,
            Integer age,
            @Size(max = 80) String country,
            @Size(max = 80) String digitalSkills,
            @Size(max = 190) String institution,
            @Size(max = 1000) String bio,
            Boolean shareProfileWithPartners,
            @Size(max = 190) String specialisation,
            @Size(max = 190) String organisation,
            @Size(max = 500) String expertise,
            @Size(max = 190) String department,
            @Size(max = 190) String organisationName,
            OrganisationType organisationType,
            @Size(max = 300) String website,
            @Size(max = 2000) String description) {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank String password) {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Email @Size(max = 190) String email) {
    }

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$", message = "Password must be 8-100 characters and include a letter and a number.") String newPassword) {
    }

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresIn,
            UserProfile user) {
    }

    public record RegisterResponse(
            Long userId,
            String name,
            String email,
            Role role,
            AccountStatus status,
            String message) {
    }

    public record MessageResponse(String message) {
    }

    public record UserProfile(
            Long userId,
            String name,
            String email,
            Role role,
            AccountStatus status,
            String phone,
            OffsetDateTime createdAt,
            OffsetDateTime lastLoginAt,
            Integer age,
            String country,
            String digitalSkills,
            boolean levelTwoEligible,
            String institution,
            String bio,
            boolean shareProfileWithPartners,
            OffsetDateTime consentDate,
            String specialisation,
            String organisation,
            String expertise,
            String department,
            String organisationName,
            OrganisationType organisationType,
            String website,
            String description,
            boolean verified) {
    }

    public record UpdateProfileRequest(
            @Size(max = 120) String name,
            @Size(max = 30) String phone,
            Integer age,
            @Size(max = 80) String country,
            @Size(max = 80) String digitalSkills,
            @Size(max = 190) String institution,
            @Size(max = 1000) String bio,
            Boolean shareProfileWithPartners,
            @Size(max = 190) String specialisation,
            @Size(max = 190) String organisation,
            @Size(max = 500) String expertise,
            @Size(max = 190) String department,
            @Size(max = 190) String organisationName,
            OrganisationType organisationType,
            @Size(max = 300) String website,
            @Size(max = 2000) String description) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,100}$", message = "Password must be 8-100 characters and include a letter and a number.") String newPassword) {
    }

    public record ConsentRequest(boolean shareProfileWithPartners) {
    }
}
