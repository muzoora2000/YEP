package com.yep.dto;

import java.time.OffsetDateTime;

import com.yep.domain.assessment.Assessment;
import com.yep.domain.enums.AssessmentTargetType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Contracts for {@code /api/v1/assessments}. */
public final class AssessmentDtos {

    private AssessmentDtos() {
    }

    public record AssessmentRequest(
            @NotNull(message = "target type is required")
            AssessmentTargetType targetType,

            Long participantId,

            Long teamId,

            @NotBlank(message = "criteria is required")
            @Size(max = 200)
            String criteria,

            @NotNull(message = "score is required")
            @Min(value = 0, message = "score must be between 0 and 100")
            @Max(value = 100, message = "score must be between 0 and 100")
            Integer score,

            @Size(max = 4000)
            String feedback) {
    }

    public record AssessmentResponse(
            Long id,
            AssessmentTargetType targetType,
            Long targetId,
            String targetLabel,
            Long assessorId,
            String assessorName,
            String criteria,
            Integer score,
            String feedback,
            OffsetDateTime assessmentDate,
            boolean pass) {

        public static AssessmentResponse of(Assessment assessment) {
            com.yep.domain.interfaces.Assessable target = assessment.resolveTarget();
            return new AssessmentResponse(
                    assessment.getId(),
                    assessment.getTargetType(),
                    target != null ? target.getId() : null,
                    target != null ? target.getAssessmentLabel() : null,
                    assessment.getAssessor() != null ? assessment.getAssessor().getId() : null,
                    assessment.getAssessor() != null ? assessment.getAssessor().getFullName() : null,
                    assessment.getCriteria(),
                    assessment.getScore(),
                    assessment.getFeedback(),
                    assessment.getAssessmentDate(),
                    assessment.isPass());
        }
    }
}
