package com.yep.domain.exchange;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.ExchangeStatus;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Abstract superclass of every activity offered on YEP.
 * {@code LevelOneExchange} and {@code LevelTwoExchange} specialise it
 * (Inheritance + Polymorphism); services accept this type whenever the two
 * levels must be handled uniformly.
 */
@Entity
@Table(
        name = "virtual_exchanges",
        indexes = {
                @Index(name = "idx_virtual_exchanges_level", columnList = "exchange_level"),
                @Index(name = "idx_virtual_exchanges_status", columnList = "status"),
                @Index(name = "idx_virtual_exchanges_start_date", columnList = "start_date")
        }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "exchange_level", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("LEVEL")
public abstract class VirtualExchange extends BaseEntity {

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "exchange_level", insertable = false, updatable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExchangeLevel level;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ExchangeStatus status = ExchangeStatus.OPEN;

    @Column(name = "organiser_id")
    private Long organiserId;

    @OneToMany(mappedBy = "virtualExchange", fetch = FetchType.LAZY)
    private Set<Enrollment> enrollments = new LinkedHashSet<>();

    /** Polymorphic helper - implemented differently by each level. */
    public abstract ExchangeLevel getLevel();

    /** Human readable one-line summary used in listings and search results. */
    public abstract String getSummaryLabel();

    public boolean isAtCapacity() {
        return false;
    }

    public int enrolledCount() {
        return enrollments == null ? 0 : enrollments.size();
    }

    public boolean isOpenForEnrollment() {
        return status == ExchangeStatus.OPEN && !isAtCapacity();
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

    public void setLevel(ExchangeLevel level) {
        this.level = level;
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

    public ExchangeStatus getStatus() {
        return status;
    }

    public void setStatus(ExchangeStatus status) {
        this.status = status;
    }

    public Long getOrganiserId() {
        return organiserId;
    }

    public void setOrganiserId(Long organiserId) {
        this.organiserId = organiserId;
    }

    public Set<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void setEnrollments(Set<Enrollment> enrollments) {
        this.enrollments = enrollments;
    }
}
