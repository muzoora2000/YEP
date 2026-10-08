package com.yep.domain.assessment;

import java.time.OffsetDateTime;
import java.util.List;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.AssessmentTargetType;
import com.yep.domain.interfaces.Assessable;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Score + feedback attached to any {@link Assessable} object.
 *
 * <p>The target is polymorphic: a single row carries {@code target_type} plus
 * exactly one of the two foreign keys ({@code participant_target_id} /
 * {@code team_target_id}), which keeps one table and one service for both
 * kinds of assessment.</p>
 */
@Entity
@Table(
        name = "assessments",
        indexes = {
                @Index(name = "idx_assessments_participant_target_id", columnList = "participant_target_id"),
                @Index(name = "idx_assessments_team_target_id", columnList = "team_target_id"),
                @Index(name = "idx_assessments_assessor_id", columnList = "assessor_id"),
                @Index(name = "idx_assessments_target_type", columnList = "target_type")
        }
)
public class Assessment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private AssessmentTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_target_id")
    private Participant participantTarget;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_target_id")
    private Team teamTarget;

    /** Facilitator, mentor or admin who produced the assessment. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "assessor_id", nullable = false)
    private User assessor;

    @Column(name = "criteria", nullable = false, length = 200)
    private String criteria;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "feedback", length = 4000)
    private String feedback;

    @Column(name = "assessment_date", nullable = false)
    private OffsetDateTime assessmentDate;

    /**
     * Polymorphic factory: binds the assessment to the correct foreign key
     * depending on the concrete kind of {@link Assessable}.
     */
    public static Assessment of(Assessable target, User assessor, String criteria, int score, String feedback) {
        Assessment assessment = new Assessment();
        assessment.targetType = target.getAssessmentTargetType();
        if (target instanceof Participant participant) {
            assessment.participantTarget = participant;
        } else if (target instanceof Team team) {
            assessment.teamTarget = team;
        } else {
            throw new IllegalArgumentException("Unsupported assessable type: " + target.getClass().getName());
        }
        assessment.assessor = assessor;
        assessment.criteria = criteria;
        assessment.score = score;
        assessment.feedback = feedback;
        assessment.assessmentDate = OffsetDateTime.now();
        return assessment;
    }

    /** Resolves the polymorphic target back through the interface. */
    public Assessable resolveTarget() {
        if (targetType == AssessmentTargetType.TEAM) {
            return teamTarget;
        }
        return participantTarget;
    }

    public boolean isPass() {
        return score != null && score >= 50;
    }

    public List<Participant> affectedParticipants() {
        Assessable target = resolveTarget();
        return target == null ? List.of() : target.getAssessmentRecipients();
    }

    public AssessmentTargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(AssessmentTargetType targetType) {
        this.targetType = targetType;
    }

    public Participant getParticipantTarget() {
        return participantTarget;
    }

    public void setParticipantTarget(Participant participantTarget) {
        this.participantTarget = participantTarget;
    }

    public Team getTeamTarget() {
        return teamTarget;
    }

    public void setTeamTarget(Team teamTarget) {
        this.teamTarget = teamTarget;
    }

    public User getAssessor() {
        return assessor;
    }

    public void setAssessor(User assessor) {
        this.assessor = assessor;
    }

    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
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
}
