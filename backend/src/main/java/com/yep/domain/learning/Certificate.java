package com.yep.domain.learning;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.user.YouthParticipant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "certificate", uniqueConstraints = @UniqueConstraint(name = "uk_certificate_number", columnNames = "certificate_number"))
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Long certificateId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "youth_id", nullable = false)
    private YouthParticipant youth;

    @Column(name = "certificate_number", nullable = false, length = 30)
    private String certificateNumber;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "achievement", nullable = false, length = 500)
    private String achievement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id")
    private LearningActivity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ImpactProject project;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected Certificate() {
    }

    public Certificate(YouthParticipant youth, String certificateNumber, String achievement) {
        this.youth = youth;
        this.certificateNumber = certificateNumber;
        this.achievement = achievement;
        this.issueDate = LocalDate.now();
    }

    public void generateCertificate(String number, String achievementText) {
        this.certificateNumber = number;
        this.achievement = achievementText;
        this.issueDate = LocalDate.now();
    }

    public void revoke() {
        this.revoked = true;
        this.revokedAt = OffsetDateTime.now();
    }

    public boolean isValid() {
        return !revoked;
    }

    public Long getCertificateId() {
        return certificateId;
    }

    public void setCertificateId(Long certificateId) {
        this.certificateId = certificateId;
    }

    public YouthParticipant getYouth() {
        return youth;
    }

    public void setYouth(YouthParticipant youth) {
        this.youth = youth;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public String getAchievement() {
        return achievement;
    }

    public void setAchievement(String achievement) {
        this.achievement = achievement;
    }

    public LearningActivity getActivity() {
        return activity;
    }

    public void setActivity(LearningActivity activity) {
        this.activity = activity;
    }

    public ImpactProject getProject() {
        return project;
    }

    public void setProject(ImpactProject project) {
        this.project = project;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(OffsetDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
