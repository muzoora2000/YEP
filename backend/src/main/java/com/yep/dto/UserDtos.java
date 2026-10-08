package com.yep.dto;

import java.time.OffsetDateTime;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.SkillLevel;
import com.yep.domain.user.Admin;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * User read/write contracts. {@link #of(User)} is polymorphic - it produces the
 * role specific projection for each concrete subclass of {@link User}.
 */
public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String fullName,
            String email,
            Role role,
            String country,
            String phone,
            AccountStatus accountStatus,
            boolean consentGiven,
            OffsetDateTime createdAt,
            Integer age,
            SkillLevel skillLevel,
            String expertiseArea,
            String expertiseTopics,
            Integer sessionsFacilitated,
            String mentorshipAreas,
            String permissions) {

        public static UserResponse of(User user) {
            Integer age = null;
            SkillLevel skillLevel = null;
            String expertiseArea = null;
            String expertiseTopics = null;
            Integer sessions = null;
            String mentorshipAreas = null;
            String permissions = null;

            if (user instanceof Participant participant) {
                age = participant.getAge();
                skillLevel = participant.getSkillLevel();
            } else if (user instanceof Facilitator facilitator) {
                expertiseArea = facilitator.getExpertiseArea();
                expertiseTopics = facilitator.getExpertiseTopics();
                sessions = facilitator.getSessionsFacilitated();
            } else if (user instanceof Mentor mentor) {
                mentorshipAreas = mentor.getMentorshipAreas();
            } else if (user instanceof Admin admin) {
                permissions = admin.getPermissions();
            }

            return new UserResponse(
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getCountry(),
                    user.getPhone(),
                    user.getAccountStatus(),
                    user.isConsentGiven(),
                    user.getCreatedAt(),
                    age,
                    skillLevel,
                    expertiseArea,
                    expertiseTopics,
                    sessions,
                    mentorshipAreas,
                    permissions);
        }
    }

    public record UpdateProfileRequest(
            @Size(max = 120) String fullName,
            @Size(max = 80) String country,
            @Size(max = 30) String phone,
            Integer age,
            SkillLevel skillLevel,
            @Size(max = 120) String expertiseArea,
            @Size(max = 400) String expertiseTopics,
            @Size(max = 400) String mentorshipAreas) {
    }

    public record AdminUpdateUserRequest(
            @Email @Size(max = 190) String email,
            @Size(max = 120) String fullName,
            AccountStatus accountStatus,
            String permissions) {
    }
}
