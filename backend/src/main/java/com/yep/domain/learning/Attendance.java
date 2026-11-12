package com.yep.domain.learning;

import java.time.OffsetDateTime;

import com.yep.domain.enums.AttendanceStatus;
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
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(name = "uk_attendance_session_youth", columnNames = { "session_id", "youth_id" }))
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private VirtualSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "youth_id", nullable = false)
    private YouthParticipant youth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "marked_at", nullable = false)
    private OffsetDateTime markedAt;

    protected Attendance() {
    }

    public Attendance(VirtualSession session, YouthParticipant youth, AttendanceStatus status) {
        this.session = session;
        this.youth = youth;
        mark(status);
    }

    public void mark(AttendanceStatus status) {
        this.status = status;
        this.markedAt = OffsetDateTime.now();
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public VirtualSession getSession() {
        return session;
    }

    public void setSession(VirtualSession session) {
        this.session = session;
    }

    public YouthParticipant getYouth() {
        return youth;
    }

    public void setYouth(YouthParticipant youth) {
        this.youth = youth;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public OffsetDateTime getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(OffsetDateTime markedAt) {
        this.markedAt = markedAt;
    }
}
