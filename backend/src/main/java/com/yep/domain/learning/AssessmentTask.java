package com.yep.domain.learning;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.TaskType;

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
@Table(name = "assessment_task")
public class AssessmentTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private LearningActivity activity;

    @Column(name = "title", nullable = false, length = 190)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private TaskType taskType;

    @Column(name = "instructions", length = 4000)
    private String instructions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questions", columnDefinition = "jsonb")
    private List<Map<String, Object>> questions;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 100;

    @Column(name = "due_date")
    private OffsetDateTime dueDate;

    @Column(name = "published", nullable = false)
    private boolean published = true;

    protected AssessmentTask() {
    }

    public AssessmentTask(LearningActivity activity, String title, TaskType taskType) {
        this.activity = activity;
        this.title = title;
        this.taskType = taskType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public List<Map<String, Object>> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Map<String, Object>> questions) {
        this.questions = questions;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public OffsetDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(OffsetDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}
