package com.yep.domain.activity;

import java.time.LocalDate;
import java.util.List;

import com.yep.domain.enums.ActivityLevel;
import com.yep.domain.enums.ActivityStatus;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.PartnerOrganisation;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Abstract learning activity. SINGLE_TABLE inheritance lets services process a
 * List&lt;LearningActivity&gt; polymorphically without instanceof checks.
 */
@Entity
@Table(name = "learning_activity")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "level", discriminatorType = DiscriminatorType.STRING, length = 10)
public abstract class LearningActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facilitator_id", nullable = false)
    private Facilitator facilitator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private PartnerOrganisation partner;

    @Column(name = "title", nullable = false, length = 190)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "category", length = 80)
    private String category;

    @Column(name = "capacity")
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ActivityStatus status = ActivityStatus.DRAFT;

    @Column(name = "pass_mark", nullable = false)
    private int passMark = 50;

    @Column(name = "min_attendance_percent", nullable = false)
    private int minAttendancePercent = 70;

    @Column(name = "min_team_size", nullable = false)
    private int minTeamSize = 3;

    @Column(name = "max_team_size", nullable = false)
    private int maxTeamSize = 5;

    protected LearningActivity() {
    }

    /** Polymorphic feature list used by the UI and API. */
    public abstract List<String> getLevelFeatures();

    /** Polymorphic policy switch: only Level Two supports team formation. */
    public abstract boolean supportsTeamFormation();

    /** Polymorphic identity of the concrete level. */
    public abstract ActivityLevel getLevel();

    public boolean isPublished() {
        return status == ActivityStatus.PUBLISHED;
    }

    public boolean hasCapacity(int enrolledCount) {
        return capacity == null || enrolledCount < capacity;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Facilitator getFacilitator() {
        return facilitator;
    }

    public void setFacilitator(Facilitator facilitator) {
        this.facilitator = facilitator;
    }

    public PartnerOrganisation getPartner() {
        return partner;
    }

    public void setPartner(PartnerOrganisation partner) {
        this.partner = partner;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public int getPassMark() {
        return passMark;
    }

    public void setPassMark(int passMark) {
        this.passMark = passMark;
    }

    public int getMinAttendancePercent() {
        return minAttendancePercent;
    }

    public void setMinAttendancePercent(int minAttendancePercent) {
        this.minAttendancePercent = minAttendancePercent;
    }

    public int getMinTeamSize() {
        return minTeamSize;
    }

    public void setMinTeamSize(int minTeamSize) {
        this.minTeamSize = minTeamSize;
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }
}
