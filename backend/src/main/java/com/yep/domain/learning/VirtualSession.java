package com.yep.domain.learning;

import java.time.OffsetDateTime;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.SessionStatus;
import com.yep.domain.user.User;

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

@Entity
@Table(name = "virtual_session")
public class VirtualSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private LearningActivity activity;

    @Column(name = "title", nullable = false, length = 190)
    private String title;

    @Column(name = "scheduled_start", nullable = false)
    private OffsetDateTime scheduledStart;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "meeting_link", nullable = false, length = 1000)
    private String meetingLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    protected VirtualSession() {
    }

    public VirtualSession(LearningActivity activity, String title, OffsetDateTime scheduledStart, int durationMinutes, String meetingLink, User createdBy) {
        this.activity = activity;
        this.title = title;
        this.scheduledStart = scheduledStart;
        this.durationMinutes = durationMinutes;
        this.meetingLink = meetingLink;
        this.createdBy = createdBy;
    }

    public boolean isCheckInOpen(OffsetDateTime now) {
        OffsetDateTime opens = scheduledStart.minusMinutes(10);
        OffsetDateTime closes = scheduledStart.plusMinutes(durationMinutes);
        return !now.isBefore(opens) && !now.isAfter(closes);
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LearningActivity getActivity() {
        return activity;
    }

    public void setActivity(LearningActivity activity) {
        this.activity = activity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getScheduledStart() {
        return scheduledStart;
    }

    public void setScheduledStart(OffsetDateTime scheduledStart) {
        this.scheduledStart = scheduledStart;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
}
