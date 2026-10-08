package com.yep.domain.message;

import java.time.OffsetDateTime;

import com.yep.domain.enums.AiModerationStatus;
import com.yep.domain.enums.AnnouncementScope;
import com.yep.domain.enums.MessageType;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.team.Team;
import com.yep.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Broadcast to a cohort, a team or every user.
 *
 * <p><b>Responsible AI:</b> announcements may be drafted with AI assistance, but an
 * AI drafted announcement is stored unpublished and only becomes visible after a
 * human facilitator/admin approves it (see {@code requiresHumanApproval}).</p>
 */
@Entity
@DiscriminatorValue("ANNOUNCEMENT")
public class Announcement extends Message {

    @Column(name = "scope", length = 30)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private AnnouncementScope scope = AnnouncementScope.ALL_USERS;

    @Column(name = "published")
    private boolean published;

    @Column(name = "requires_human_approval")
    private boolean requiresHumanApproval;

    @Column(name = "approved_by_id")
    private Long approvedById;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    public static Announcement byUser(User sender, AnnouncementScope scope, String subject, String body,
                                      boolean aiGenerated) {
        Announcement announcement = new Announcement();
        announcement.setSender(sender);
        announcement.setScope(scope);
        announcement.setSubject(subject);
        announcement.setBody(body);
        announcement.setAiGenerated(aiGenerated);
        announcement.setSentAt(OffsetDateTime.now());
        announcement.setRequiresHumanApproval(aiGenerated);
        announcement.setPublished(!aiGenerated);
        if (aiGenerated) {
            announcement.setAiModerationStatus(AiModerationStatus.NOT_SCREENED);
        }
        return announcement;
    }

    public static Announcement forExchange(User sender, VirtualExchange exchange, String subject, String body,
                                           boolean aiGenerated) {
        Announcement announcement = byUser(sender, AnnouncementScope.EXCHANGE_COHORT, subject, body, aiGenerated);
        announcement.setExchange(exchange);
        return announcement;
    }

    public static Announcement forTeam(User sender, Team team, String subject, String body, boolean aiGenerated) {
        Announcement announcement = byUser(sender, AnnouncementScope.TEAM, subject, body, aiGenerated);
        announcement.setTeam(team);
        return announcement;
    }

    /** Human approval step - only a person can flip this flag. */
    public void approveBy(User approver) {
        this.published = true;
        this.requiresHumanApproval = false;
        this.approvedById = approver != null ? approver.getId() : null;
        this.approvedAt = OffsetDateTime.now();
        if (this.getAiModerationStatus() == AiModerationStatus.NOT_SCREENED
                || this.getAiModerationStatus() == AiModerationStatus.FLAGGED) {
            this.setAiModerationStatus(AiModerationStatus.OVERRIDDEN_BY_HUMAN);
        }
    }

    public boolean isVisible() {
        return published && !requiresHumanApproval;
    }

    public AnnouncementScope getScope() {
        return scope;
    }

    public void setScope(AnnouncementScope scope) {
        this.scope = scope;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isRequiresHumanApproval() {
        return requiresHumanApproval;
    }

    public void setRequiresHumanApproval(boolean requiresHumanApproval) {
        this.requiresHumanApproval = requiresHumanApproval;
    }

    public Long getApprovedById() {
        return approvedById;
    }

    public void setApprovedById(Long approvedById) {
        this.approvedById = approvedById;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.ANNOUNCEMENT;
    }
}
