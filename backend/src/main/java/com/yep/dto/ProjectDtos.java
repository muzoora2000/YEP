package com.yep.dto;

import java.time.OffsetDateTime;

import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.project.ImpactProject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contracts for {@code /api/v1/projects}. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectRequest(
            @NotBlank(message = "title is required")
            @Size(max = 180)
            String title,

            @Size(max = 4000)
            String problemStatement,

            @Size(max = 4000)
            String proposedSolution,

            Boolean aiAssisted,

            @Size(max = 2000)
            String aiUsageNote) {
    }

    public record ProjectResponse(
            Long id,
            String title,
            String problemStatement,
            String proposedSolution,
            Long teamId,
            String teamName,
            Long levelTwoExchangeId,
            String focusArea,
            ProjectStatus status,
            OffsetDateTime submissionDate,
            boolean aiAssisted,
            String aiUsageNote,
            String reviewFeedback,
            OffsetDateTime reviewedAt) {

        public static ProjectResponse of(ImpactProject project) {
            return new ProjectResponse(
                    project.getId(),
                    project.getTitle(),
                    project.getProblemStatement(),
                    project.getProposedSolution(),
                    project.getTeam() != null ? project.getTeam().getId() : null,
                    project.getTeam() != null ? project.getTeam().getName() : null,
                    project.getTeam() != null && project.getTeam().getLevelTwoExchange() != null
                            ? project.getTeam().getLevelTwoExchange().getId() : null,
                    project.getTeam() != null && project.getTeam().getLevelTwoExchange() != null
                            ? project.getTeam().getLevelTwoExchange().getFocusArea().name() : null,
                    project.getStatus(),
                    project.getSubmissionDate(),
                    project.isAiAssisted(),
                    project.getAiUsageNote(),
                    project.getReviewFeedback(),
                    project.getReviewedAt());
        }
    }

    public record ProjectReviewRequest(
            @NotBlank
            String decision,
            @Size(max = 4000)
            String feedback) {
    }
}
