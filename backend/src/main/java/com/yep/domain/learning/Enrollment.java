package com.yep.domain.learning;

import java.time.OffsetDateTime;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.user.YouthParticipant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "enrollment", uniqueConstraints = @UniqueConstraint(name = "uk_enrollment_youth_activity", columnNames = { "youth_id", "activity_id" }))
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "youth_id", nullable = false)
    private YouthParticipant youth;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private LearningActivity activity;

    @Column(name = "enrollment_date", nullable = false)
    private OffsetDateTime enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EnrollmentStatus status;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected Enrollment() {
    }

    public Enrollment(YouthParticipant youth, LearningActivity activity) {
        this.youth = youth;
        this.activity = activity;
        this.enrollmentDate = OffsetDateTime.now();
        this.status = EnrollmentStatus.APPLIED;
    }

    /** Encapsulation: status only changes through explicit domain operations. */
    public void confirmEnrollment() {
        if (status == EnrollmentStatus.CANCELLED || status == EnrollmentStatus.REJECTED
                || status == EnrollmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot confirm an enrollment in status " + status);
        }
        this.status = EnrollmentStatus.ENROLLED;
    }

    public void cancelEnrollment() {
        if (status == EnrollmentStatus.COMPLETED) {
            throw new IllegalStateException("Completed enrollment cannot be cancelled");
        }
        this.status = EnrollmentStatus.CANCELLED;
    }

    public void waitlistEnrollment() {
        this.status = EnrollmentStatus.WAITLISTED;
    }

    public void rejectEnrollment() {
        this.status = EnrollmentStatus.REJECTED;
    }

    public void completeEnrollment() {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = OffsetDateTime.now();
    }

    public boolean isActive() {
        return status == EnrollmentStatus.ENROLLED;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public YouthParticipant getYouth() {
        return youth;
    }

    public void setYouth(YouthParticipant youth) {
        this.youth = youth;
    }

    public LearningActivity getActivity() {
        return activity;
    }

    public void setActivity(LearningActivity activity) {
        this.activity = activity;
    }

    public OffsetDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(OffsetDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
