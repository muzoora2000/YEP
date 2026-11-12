package com.yep.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.yep.domain.enums.OpportunityStatus;
import com.yep.domain.enums.OpportunityType;
import com.yep.domain.enums.OrganisationType;
import com.yep.domain.enums.SupportStatus;
import com.yep.domain.enums.SupportType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class PartnerDtos {

    private PartnerDtos() {
    }

    public record OpportunityRequest(
            @NotBlank @Size(max = 190) String title,
            @NotNull OpportunityType opportunityType,
            @Size(max = 4000) String description,
            @Size(max = 190) String location,
            LocalDate deadline) {
    }

    public record OpportunityResponse(
            Long opportunityId,
            Long partnerId,
            String partnerName,
            String organisationName,
            String title,
            OpportunityType opportunityType,
            String description,
            String location,
            LocalDate deadline,
            OpportunityStatus status,
            OffsetDateTime createdAt,
            long interestCount) {
    }

    public record InterestRequest(@Size(max = 2000) String message) {
    }

    public record InterestResponse(
            Long interestId,
            Long opportunityId,
            String opportunityTitle,
            Long youthId,
            String youthName,
            String youthEmail,
            String message,
            OffsetDateTime createdAt) {
    }

    public record SupportRequest(
            @NotNull SupportType supportType,
            @Size(max = 2000) String message) {
    }

    public record SupportDecisionRequest(boolean accept) {
    }

    public record SupportResponse(
            Long supportId,
            Long projectId,
            String projectTitle,
            Long partnerId,
            String partnerName,
            String organisationName,
            SupportType supportType,
            String message,
            SupportStatus status,
            OffsetDateTime createdAt) {
    }

    public record PartnerSummary(
            Long partnerId,
            String name,
            String email,
            String organisationName,
            String country,
            OrganisationType organisationType,
            String website,
            String description,
            boolean verified) {
    }

    public record PartnerVerificationRequest(boolean verified) {
    }

    public record PublicSettingsResponse(String platformName, List<String> countries) {
    }

    public record NotificationResponse(
            Long notificationId,
            com.yep.domain.enums.NotificationType type,
            String message,
            String link,
            OffsetDateTime dateSent,
            boolean read) {
    }

    public record UnreadCountResponse(long unread) {
    }

    public record SendMessageRequest(
            @NotNull Long recipientId,
            @NotBlank @Size(max = 4000) String message,
            @Size(max = 500) String link) {
    }

    public record AuditLogResponse(
            Long logId,
            Long userId,
            String userName,
            String action,
            String entity,
            Long entityId,
            String details,
            OffsetDateTime createdAt) {
    }

    public record DashboardResponse(
            long totalUsers,
            long participants,
            long facilitators,
            long mentors,
            long partners,
            long administrators,
            long pendingUsers,
            long activeUsers,
            long activities,
            long publishedActivities,
            long enrollments,
            long teams,
            long projects,
            long approvedProjects,
            long certificates,
            long opportunities,
            long verifiedPartners) {
    }

    public record SettingResponse(String key, String value, Long updatedBy, OffsetDateTime updatedAt) {
    }

    public record SettingUpdateRequest(@NotBlank @Size(max = 4000) String value) {
    }
}
