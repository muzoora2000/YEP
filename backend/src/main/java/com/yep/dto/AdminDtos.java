package com.yep.dto;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.OrganisationType;
import com.yep.domain.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record CreateUserRequest(
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
            @Size(max = 190) String specialisation,
            @Size(max = 190) String organisation,
            @Size(max = 500) String expertise,
            @Size(max = 190) String department,
            @Size(max = 190) String organisationName,
            OrganisationType organisationType,
            @Size(max = 300) String website,
            @Size(max = 2000) String description) {
    }

    public record StatusChangeRequest(@NotNull AccountStatus status) {
    }

    public record ApprovalRequest(boolean approved) {
    }

    public record RoleChangeRequest(@NotNull Role role) {
    }
}
