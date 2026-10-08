package com.yep.domain.message;

import java.time.OffsetDateTime;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.MessageType;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.team.Team;
import com.yep.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Abstract communication object shared by direct messages, team messages,
 * system notifications and announcements ({@code messages} table,
 * {@code message_type} discriminator).
 */
@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_messages_sender_id", columnList = "sender_id"),
                @Index(name = "idx_messages_recipient_id", columnList = "recipient_id"),
                @Index(name = "idx_messages_team_id", columnList = "team_id"),
                @Index(name = "idx_messages_exchange_id", columnList = "exchange_id"),
                @Index(name = "idx_messages_message_type", columnList = "message_type")
        }
)
@jakarta.persistence.Inheritance(strategy = jakarta.persistence.InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "message_type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("MESSAGE")
public abstract class Message extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id")
    private VirtualExchange exchange;

    @Column(name = "subject", length = 200)
    private String subject;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;

    // ----- Responsible AI flags -------------------------------------------------

    /** true when the payload was drafted or rewritten by an AI tool. */
    @Column(name = "ai_generated", nullable = false)
    private boolean aiGenerated;

    /** Why an AI moderation pass flagged this message (null when clean). */
    @Column(name = "ai_flag_reason", length = 500)
    private String aiFlagReason;

    /** AI moderation outcome - always reviewable by a human before action. */
    @Column(name = "ai_moderation_status", length = 30)
    @Enumerated(EnumType.STRING)
    private com.yep.domain.enums.AiModerationStatus aiModerationStatus;

    /** Polymorphic type tag used by the API layer. */
    public abstract MessageType getMessageType();

    public void markRead() {
        this.read = true;
    }

    public boolean isFromSystem() {
        return sender == null;
    }

    protected void touch() {
        this.sentAt = OffsetDateTime.now();
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public VirtualExchange getExchange() {
        return exchange;
    }

    public void setExchange(VirtualExchange exchange) {
        this.exchange = exchange;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(OffsetDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public String getAiFlagReason() {
        return aiFlagReason;
    }

    public void setAiFlagReason(String aiFlagReason) {
        this.aiFlagReason = aiFlagReason;
    }

    public com.yep.domain.enums.AiModerationStatus getAiModerationStatus() {
        return aiModerationStatus;
    }

    public void setAiModerationStatus(com.yep.domain.enums.AiModerationStatus aiModerationStatus) {
        this.aiModerationStatus = aiModerationStatus;
    }
}
