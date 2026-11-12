package com.yep.domain.partner;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.yep.domain.enums.OpportunityStatus;
import com.yep.domain.enums.OpportunityType;
import com.yep.domain.user.PartnerOrganisation;

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
@Table(name = "opportunity")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "opportunity_id")
    private Long opportunityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerOrganisation partner;

    @Column(name = "title", nullable = false, length = 190)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "opportunity_type", nullable = false, length = 20)
    private OpportunityType opportunityType;

    @Column(name = "description", length = 4000)
    private String description;

    @Column(name = "location", length = 190)
    private String location;

    @Column(name = "deadline")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OpportunityStatus status = OpportunityStatus.OPEN;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Opportunity() {
    }

    public Opportunity(PartnerOrganisation partner, String title, OpportunityType opportunityType) {
        this.partner = partner;
        this.title = title;
        this.opportunityType = opportunityType;
        this.createdAt = OffsetDateTime.now();
    }

    public void close() {
        this.status = OpportunityStatus.CLOSED;
    }

    public Long getOpportunityId() {
        return opportunityId;
    }

    public void setOpportunityId(Long opportunityId) {
        this.opportunityId = opportunityId;
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

    public OpportunityType getOpportunityType() {
        return opportunityType;
    }

    public void setOpportunityType(OpportunityType opportunityType) {
        this.opportunityType = opportunityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
