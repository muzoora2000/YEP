package com.yep.domain.project;

import java.time.OffsetDateTime;

import com.yep.domain.enums.SupportStatus;
import com.yep.domain.enums.SupportType;
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
@Table(name = "project_support")
public class ProjectSupport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "support_id")
    private Long supportId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ImpactProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private PartnerOrganisation partner;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_type", nullable = false, length = 20)
    private SupportType supportType;

    @Column(name = "message", length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupportStatus status = SupportStatus.OFFERED;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ProjectSupport() {
    }

    public ProjectSupport(ImpactProject project, PartnerOrganisation partner, SupportType supportType, String message) {
        this.project = project;
        this.partner = partner;
        this.supportType = supportType;
        this.message = message;
        this.createdAt = OffsetDateTime.now();
    }

    public void accept() {
        this.status = SupportStatus.ACCEPTED;
    }

    public void decline() {
        this.status = SupportStatus.DECLINED;
    }

    public Long getSupportId() {
        return supportId;
    }

    public void setSupportId(Long supportId) {
        this.supportId = supportId;
    }

    public ImpactProject getProject() {
        return project;
    }

    public void setProject(ImpactProject project) {
        this.project = project;
    }

    public PartnerOrganisation getPartner() {
        return partner;
    }

    public void setPartner(PartnerOrganisation partner) {
        this.partner = partner;
    }

    public SupportType getSupportType() {
        return supportType;
    }

    public void setSupportType(SupportType supportType) {
        this.supportType = supportType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SupportStatus getStatus() {
        return status;
    }

    public void setStatus(SupportStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
