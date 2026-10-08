package com.yep.domain.project;

import java.time.OffsetDateTime;
import java.util.List;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.interfaces.Certifiable;
import com.yep.domain.team.Team;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * The real-world problem a level two team is solving.
 * Implements {@link Certifiable}: a completed project generates certificates
 * for every member of its team.
 */
@Entity
@Table(
        name = "impact_projects",
        indexes = {
                @Index(name = "idx_impact_projects_team_id", columnList = "team_id"),
                @Index(name = "idx_impact_projects_status", columnList = "status")
        }
)
public class ImpactProject extends BaseEntity implements Certifiable {

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "problem_statement", length = 4000)
    private String problemStatement;

    @Column(name = "proposed_solution", length = 4000)
    private String proposedSolution;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false, unique = true)
    private Team team;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column(name = "submission_date")
    private OffsetDateTime submissionDate;

    /** Responsible-AI labelling: content assisted by an AI tool must be flagged. */
    @Column(name = "ai_assisted", nullable = false)
    private boolean aiAssisted;

    /** Written statement of how AI was used and what the human reviewer changed. */
    @Column(name = "ai_usage_note", length = 2000)
    private String aiUsageNote;

    @Column(name = "review_feedback", length = 4000)
    private String reviewFeedback;

    @Column(name = "reviewed_by_id")
    private Long reviewedById;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    public static ImpactProject draftFor(Team team, String title) {
        ImpactProject project = new ImpactProject();
        project.team = team;
        project.title = title;
        project.status = ProjectStatus.DRAFT;
        return project;
    }

    public void submit() {
        this.status = ProjectStatus.SUBMITTED;
        this.submissionDate = OffsetDateTime.now();
    }

    public void startReview() {
        this.status = ProjectStatus.UNDER_REVIEW;
    }

    public void approve(String feedback, User reviewer) {
        this.status = ProjectStatus.APPROVED;
        this.reviewFeedback = feedback;
        this.reviewedById = reviewer != null ? reviewer.getId() : null;
        this.reviewedAt = OffsetDateTime.now();
    }

    public void complete() {
        this.status = ProjectStatus.COMPLETED;
    }

    public boolean canBeSubmitted() {
        return status == ProjectStatus.DRAFT || status == ProjectStatus.SUBMITTED;
    }

    public boolean isCompleted() {
        return status == ProjectStatus.COMPLETED;
    }

    // ---------------------------------------------------------------------
    // Certifiable
    // ---------------------------------------------------------------------

    @Override
    public String getCertificateTitle() {
        return "Impact Project Completion - " + title;
    }

    @Override
    public String getCertificateReference() {
        return title;
    }

    @Override
    public boolean qualifiesForCertificate() {
        return isCompleted() && team != null && !team.memberParticipants().isEmpty();
    }

    @Override
    public List<Participant> getCertificateRecipients() {
        if (!qualifiesForCertificate()) {
            return List.of();
        }
        return team.memberParticipants().stream().toList();
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProblemStatement() {
        return problemStatement;
    }

    public void setProblemStatement(String problemStatement) {
        this.problemStatement = problemStatement;
    }

    public String getProposedSolution() {
        return proposedSolution;
    }

    public void setProposedSolution(String proposedSolution) {
        this.proposedSolution = proposedSolution;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public OffsetDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(OffsetDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public boolean isAiAssisted() {
        return aiAssisted;
    }

    public void setAiAssisted(boolean aiAssisted) {
        this.aiAssisted = aiAssisted;
    }

    public String getAiUsageNote() {
        return aiUsageNote;
    }

    public void setAiUsageNote(String aiUsageNote) {
        this.aiUsageNote = aiUsageNote;
    }

    public String getReviewFeedback() {
        return reviewFeedback;
    }

    public void setReviewFeedback(String reviewFeedback) {
        this.reviewFeedback = reviewFeedback;
    }

    public Long getReviewedById() {
        return reviewedById;
    }

    public void setReviewedById(Long reviewedById) {
        this.reviewedById = reviewedById;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
