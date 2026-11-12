package com.yep.domain.learning;

import java.time.OffsetDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.AssessmentStatus;
import com.yep.domain.user.User;
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
@Table(name = "assessment", uniqueConstraints = @UniqueConstraint(name = "uk_assessment_task_youth", columnNames = { "task_id", "youth_id" }))
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assessment_id")
    private Long assessmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private LearningActivity activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "youth_id", nullable = false)
    private YouthParticipant youth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private AssessmentTask task;

    @Column(name = "score")
    private Integer score;

    @Column(name = "feedback", length = 4000)
    private String feedback;

    @Column(name = "assessment_date")
    private OffsetDateTime assessmentDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers", columnDefinition = "jsonb")
    private List<Integer> answers;

    @Column(name = "submission_text", length = 6000)
    private String submissionText;

    @Column(name = "max_score")
    private Integer maxScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssessmentStatus status = AssessmentStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    protected Assessment() {
    }

    public Assessment(LearningActivity activity, YouthParticipant youth, AssessmentTask task) {
        this.activity = activity;
        this.youth = youth;
        this.task = task;
        this.status = AssessmentStatus.SUBMITTED;
        this.assessmentDate = OffsetDateTime.now();
    }

    public void evaluate(int score, String feedback) {
        this.score = score;
        this.feedback = feedback;
        if (this.assessmentDate == null) {
            this.assessmentDate = OffsetDateTime.now();
        }
        this.status = AssessmentStatus.GRADED;
    }

    public void provideFeedback(String feedback) {
        this.feedback = feedback;
    }

    public boolean isGraded() {
        return status == AssessmentStatus.GRADED;
    }

    public double percentage() {
        if (score == null || maxScore == null || maxScore == 0) {
            return 0.0;
        }
        return (score * 100.0) / maxScore;
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Long assessmentId) {
        this.assessmentId = assessmentId;
    }

    public LearningActivity getActivity() {
        return activity;
    }

    public void setActivity(LearningActivity activity) {
        this.activity = activity;
    }

    public YouthParticipant getYouth() {
        return youth;
    }

    public void setYouth(YouthParticipant youth) {
        this.youth = youth;
    }

    public AssessmentTask getTask() {
        return task;
    }

    public void setTask(AssessmentTask task) {
        this.task = task;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public OffsetDateTime getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(OffsetDateTime assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    public List<Integer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Integer> answers) {
        this.answers = answers;
    }

    public String getSubmissionText() {
        return submissionText;
    }

    public void setSubmissionText(String submissionText) {
        this.submissionText = submissionText;
    }

    public Integer getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore) {
        this.maxScore = maxScore;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public void setStatus(AssessmentStatus status) {
        this.status = status;
    }

    public User getGradedBy() {
        return gradedBy;
    }

    public void setGradedBy(User gradedBy) {
        this.gradedBy = gradedBy;
    }
}
