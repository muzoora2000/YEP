package com.yep.domain.notification;

import com.yep.domain.enums.NotificationType;
import com.yep.domain.user.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMAIL")
public class EmailNotification extends Notification {

    protected EmailNotification() {
    }

    public EmailNotification(User recipient, String message, String link) {
        super(recipient, message, link);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.EMAIL;
    }

    @Override
    public String send() {
        return "Email notification queued for " + getRecipient().getEmail();
    }
}
