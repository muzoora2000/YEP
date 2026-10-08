package com.yep.domain.exchange;

import java.time.OffsetDateTime;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.CompletionStatus;
import com.yep.domain.user.Participant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Join entity between a {@link Participant} and a {@link VirtualExchange}.
 * Captures when the enrollment happened and how it finished.
 */
@Entity
@Table(
        name = "enrollments",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollments_participant_exchange",
                columnNames = { "participant_id", "virtual_exchange_id" }
        ),
        indexes = {
                @Index(name = "idx_enrollments_virtual_exchange_id", columnList = "virtual_exchange_id"),
                @Index(name = "idx_enrollments_participant_id", columnList = "participant_id"),
                @Index(name = "idx_enrollments_completion_status", columnList = "completion_status")
        }
)
public class Enrollment extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_exchange_id", nullable = false)
    private VirtualExchange virtualExchange;

    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime enrolledAt;

    @Column(name = "completion_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private CompletionStatus completionStatus = CompletionStatus.IN_PROGRESS;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    public static Enrollment open(Participant participant, VirtualExchange exchange) {
        Enrollment enrollment = new Enrollment();
        enrollment.participant = participant;
        enrollment.virtualExchange = exchange;
        enrollment.enrolledAt = OffsetDateTime.now();
        enrollment.completionStatus = CompletionStatus.IN_PROGRESS;
        return enrollment;
    }

    public void markCompleted() {
        this.completionStatus = CompletionStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public void markDropped() {
        this.completionStatus = CompletionStatus.DROPPED;
    }

    public boolean isFinished() {
        return completionStatus != CompletionStatus.IN_PROGRESS;
    }

    public Participant getParticipant() {
        return participant;
    }

    public void setParticipant(Participant participant) {
        this.participant = participant;
    }

    public VirtualExchange getVirtualExchange() {
        return virtualExchange;
    }

    public void setVirtualExchange(VirtualExchange virtualExchange) {
        this.virtualExchange = virtualExchange;
    }

    public OffsetDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(OffsetDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public CompletionStatus getCompletionStatus() {
        return completionStatus;
    }

    public void setCompletionStatus(CompletionStatus completionStatus) {
        this.completionStatus = completionStatus;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
