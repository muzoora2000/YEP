package com.yep.domain.notification;

import java.time.OffsetDateTime;

import com.yep.domain.enums.NotificationType;
import com.yep.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
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
 * Abstract notification root. NotificationService depends on this type and calls
 * send() polymorphically; the factory chooses the concrete delivery sender.
 */
@Entity
@Table(name = "notification")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User recipient;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "date_sent", nullable = false)
    private OffsetDateTime dateSent;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "link", length = 500)
    private String link;

    protected Notification() {
    }

    protected Notification(User recipient, String message, String link) {
        this.recipient = recipient;
        this.message = message;
        this.link = link;
        this.dateSent = OffsetDateTime.now();
    }

    public abstract NotificationType getType();

    public abstract String send();

    public void markRead() {
        this.read = true;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getDateSent() {
        return dateSent;
    }

    public void setDateSent(OffsetDateTime dateSent) {
        this.dateSent = dateSent;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
