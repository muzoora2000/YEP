package com.yep.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import com.yep.domain.enums.ChallengeArea;
import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.ReviewDecision;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.enums.TeamStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record TeamRequest(
            @NotNull Long activityId,
            @NotBlank @Size(max = 120) String teamName,
            Long mentorId) {
    }

    public record AddTeamMemberRequest(Long youthId) {
    }

    public record MentorAssignmentRequest(@NotNull Long mentorId) {
    }

    public record TeamMemberResponse(
            Long memberId,
            Long youthId,
            String name,
            String email,
            TeamMemberRole role) {
    }

    public record TeamResponse(
            Long teamId,
            Long activityId,
            String activityTitle,
            String teamName,
            Long mentorId,
            String mentorName,
            Long createdById,
            String createdByName,
            TeamStatus status,
            int memberCount,
            int minTeamSize,
            int maxTeamSize,
            List<TeamMemberResponse> members,
            Long projectId,
            String projectTitle) {
    }

    public record ProjectRequest(
            @NotNull Long teamId,
            @NotBlank @Size(max = 190) String title,
            @Size(max = 6000) String description,
            @NotNull ChallengeArea challengeArea,
            Boolean aiUsed,
            @Size(max = 2000) String aiUsageDeclaration) {
    }

    public record ProjectUpdateRequest(
            @Size(max = 190) String title,
            @Size(max = 6000) String description,
            ChallengeArea challengeArea,
            Boolean aiUsed,
            @Size(max = 2000) String aiUsageDeclaration) {
    }

    public record MilestoneRequest(
            @NotBlank @Size(max = 190) String title,
            LocalDate dueDate) {
    }

    public record MilestoneStatusRequest(@NotNull MilestoneStatus status) {
    }

    public record MilestoneResponse(
            Long milestoneId,
            Long projectId,
            String title,
            LocalDate dueDate,
            MilestoneStatus status,
            OffsetDateTime completedAt,
            boolean overdue) {
    }

    public record ReviewRequest(
            @NotNull ReviewDecision decision,
            @NotBlank @Size(max = 4000) String feedback) {
    }

    public record ReviewResponse(
            Long reviewId,
            Long projectId,
            Long mentorId,
            String mentorName,
            ReviewDecision decision,
            String feedback,
            OffsetDateTime createdAt) {
    }

    public record ProjectResponse(
            Long projectId,
            Long teamId,
            String teamName,
            Long activityId,
            String activityTitle,
            String title,
            String description,
            ChallengeArea challengeArea,
            ProjectStatus status,
            boolean aiUsed,
            String aiUsageDeclaration,
            OffsetDateTime submittedAt,
            boolean openForSupport,
            int progressPercent,
            boolean overdue,
            List<MilestoneResponse> milestones,
            List<ReviewResponse> reviews) {
    }

    public record SupportVisibilityRequest(boolean openForSupport) {
    }
}
