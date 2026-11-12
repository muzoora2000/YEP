package com.yep.domain.project;

import java.time.OffsetDateTime;

import com.yep.domain.enums.ReviewDecision;
import com.yep.domain.user.Mentor;

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
@Table(name = "project_review")
public class ProjectReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ImpactProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private ReviewDecision decision;

    @Column(name = "feedback", nullable = false, length = 4000)
    private String feedback;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ProjectReview() {
    }

    public ProjectReview(ImpactProject project, Mentor mentor, ReviewDecision decision, String feedback) {
        this.project = project;
        this.mentor = mentor;
        this.decision = decision;
        this.feedback = feedback;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public ImpactProject getProject() {
        return project;
    }

    public void setProject(ImpactProject project) {
        this.project = project;
    }

    public Mentor getMentor() {
        return mentor;
    }

    public void setMentor(Mentor mentor) {
        this.mentor = mentor;
    }

    public ReviewDecision getDecision() {
        return decision;
    }

    public void setDecision(ReviewDecision decision) {
        this.decision = decision;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
