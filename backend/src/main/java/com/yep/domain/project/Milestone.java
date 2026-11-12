package com.yep.domain.project;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.yep.domain.enums.MilestoneStatus;

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
@Table(name = "milestone")
public class Milestone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "milestone_id")
    private Long milestoneId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ImpactProject project;

    @Column(name = "title", nullable = false, length = 190)
    private String title;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MilestoneStatus status = MilestoneStatus.PENDING;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected Milestone() {
    }

    public Milestone(ImpactProject project, String title, LocalDate dueDate) {
        this.project = project;
        this.title = title;
        this.dueDate = dueDate;
    }

    public void updateMilestone(MilestoneStatus status) {
        this.status = status;
        this.completedAt = status == MilestoneStatus.DONE ? OffsetDateTime.now() : null;
    }

    public boolean isOverdue(LocalDate today) {
        return dueDate != null && dueDate.isBefore(today) && status != MilestoneStatus.DONE;
    }

    public Long getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
    }

    public ImpactProject getProject() {
        return project;
    }

    public void setProject(ImpactProject project) {
        this.project = project;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public MilestoneStatus getStatus() {
        return status;
    }

    public void setStatus(MilestoneStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
