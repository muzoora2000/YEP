package com.yep.domain.project;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.yep.domain.enums.ChallengeArea;
import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.enums.ProjectStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "impact_project")
public class ImpactProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "title", nullable = false, length = 190)
    private String title;

    @Column(name = "description", length = 6000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_area", nullable = false, length = 40)
    private ChallengeArea challengeArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProjectStatus status = ProjectStatus.DRAFT;

    @Column(name = "ai_used", nullable = false)
    private boolean aiUsed = false;

    @Column(name = "ai_usage_declaration", length = 2000)
    private String aiUsageDeclaration;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "open_for_support", nullable = false)
    private boolean openForSupport = false;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Milestone> milestones = new ArrayList<>();

    protected ImpactProject() {
    }

    public ImpactProject(Team team, String title) {
        this.team = team;
        this.title = title;
    }

    public void createProject(String description, ChallengeArea challengeArea) {
        this.description = description;
        this.challengeArea = challengeArea;
        this.status = ProjectStatus.DRAFT;
    }

    public void updateProject(String title, String description, ChallengeArea challengeArea) {
        if (!isEditable()) {
            throw new IllegalStateException("Project can only be edited while DRAFT or CHANGES_REQUESTED");
        }
        this.title = title;
        this.description = description;
        this.challengeArea = challengeArea;
    }

    public void submitProject() {
        if (milestones.size() < 2) {
            throw new IllegalStateException("At least two milestones are required before submission");
        }
        if (aiUsed && (aiUsageDeclaration == null || aiUsageDeclaration.isBlank())) {
            throw new IllegalStateException("An AI usage declaration is required when AI was used");
        }
        this.status = ProjectStatus.SUBMITTED;
        this.submittedAt = OffsetDateTime.now();
    }

    public void openForReview() {
        if (status != ProjectStatus.SUBMITTED) {
            throw new IllegalStateException("Only a submitted project can be opened for review");
        }
        this.status = ProjectStatus.UNDER_REVIEW;
    }

    public void requestChanges() {
        this.status = ProjectStatus.CHANGES_REQUESTED;
    }

    public void approve() {
        this.status = ProjectStatus.APPROVED;
    }

    public void complete() {
        this.status = ProjectStatus.COMPLETED;
    }

    public boolean isEditable() {
        return status == ProjectStatus.DRAFT || status == ProjectStatus.CHANGES_REQUESTED;
    }

    public int progressPercent() {
        if (milestones.isEmpty()) {
            return 0;
        }
        long done = milestones.stream().filter(m -> m.getStatus() == MilestoneStatus.DONE).count();
        return (int) Math.round((done * 100.0) / milestones.size());
    }

    public boolean hasOverdueMilestone(LocalDate today) {
        return milestones.stream().anyMatch(m -> m.isOverdue(today));
    }

    public void addMilestone(Milestone milestone) {
        milestones.add(milestone);
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ChallengeArea getChallengeArea() {
        return challengeArea;
    }

    public void setChallengeArea(ChallengeArea challengeArea) {
        this.challengeArea = challengeArea;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public boolean isAiUsed() {
        return aiUsed;
    }

    public void setAiUsed(boolean aiUsed) {
        this.aiUsed = aiUsed;
    }

    public String getAiUsageDeclaration() {
        return aiUsageDeclaration;
    }

    public void setAiUsageDeclaration(String aiUsageDeclaration) {
        this.aiUsageDeclaration = aiUsageDeclaration;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public boolean isOpenForSupport() {
        return openForSupport;
    }

    public void setOpenForSupport(boolean openForSupport) {
        this.openForSupport = openForSupport;
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public void setMilestones(List<Milestone> milestones) {
        this.milestones = milestones;
    }
}
