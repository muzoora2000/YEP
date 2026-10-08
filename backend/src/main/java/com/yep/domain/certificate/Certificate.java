package com.yep.domain.certificate;

import java.time.OffsetDateTime;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.user.Participant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A recognisation document issued to a participant.
 * {@code verificationCode} is the public, non-guessable handle used by the
 * verification page - it never exposes internal ids.
 */
@Entity
@Table(
        name = "certificates",
        indexes = {
                @Index(name = "idx_certificates_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_certificates_verification_code", columnList = "verification_code"),
                @Index(name = "idx_certificates_issue_date", columnList = "issue_date")
        }
)
public class Certificate extends BaseEntity {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Participant recipient;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "issue_date", nullable = false)
    private OffsetDateTime issueDate;

    @Column(name = "verification_code", nullable = false, length = 64, unique = true)
    private String verificationCode;

    @Column(name = "related_activity_or_project", length = 300)
    private String relatedActivityOrProject;

    public static Certificate issuedTo(Participant recipient, String title, String reference, String code) {
        Certificate certificate = new Certificate();
        certificate.recipient = recipient;
        certificate.title = title;
        certificate.relatedActivityOrProject = reference;
        certificate.verificationCode = code;
        certificate.issueDate = OffsetDateTime.now();
        return certificate;
    }

    public boolean isIssuedOnOrAfter(OffsetDateTime moment) {
        return issueDate != null && !issueDate.isBefore(moment);
    }

    public Participant getRecipient() {
        return recipient;
    }

    public void setRecipient(Participant recipient) {
        this.recipient = recipient;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public OffsetDateTime getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(OffsetDateTime issueDate) {
        this.issueDate = issueDate;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getRelatedActivityOrProject() {
        return relatedActivityOrProject;
    }

    public void setRelatedActivityOrProject(String relatedActivityOrProject) {
        this.relatedActivityOrProject = relatedActivityOrProject;
    }
}
