package com.yep.dto;

import java.time.OffsetDateTime;

import com.yep.domain.enums.CompletionStatus;
import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.exchange.Enrollment;

/** Contracts for enrollment into a virtual exchange. */
public final class EnrollmentDtos {

    private EnrollmentDtos() {
    }

    public record EnrollmentResponse(
            Long id,
            Long participantId,
            String participantName,
            Long exchangeId,
            String exchangeTitle,
            ExchangeLevel level,
            OffsetDateTime enrolledAt,
            CompletionStatus completionStatus,
            OffsetDateTime completedAt) {

        public static EnrollmentResponse of(Enrollment enrollment) {
            return new EnrollmentResponse(
                    enrollment.getId(),
                    enrollment.getParticipant() != null ? enrollment.getParticipant().getId() : null,
                    enrollment.getParticipant() != null ? enrollment.getParticipant().getFullName() : null,
                    enrollment.getVirtualExchange() != null ? enrollment.getVirtualExchange().getId() : null,
                    enrollment.getVirtualExchange() != null ? enrollment.getVirtualExchange().getTitle() : null,
                    enrollment.getVirtualExchange() != null ? enrollment.getVirtualExchange().getLevel() : null,
                    enrollment.getEnrolledAt(),
                    enrollment.getCompletionStatus(),
                    enrollment.getCompletedAt());
        }
    }
}
