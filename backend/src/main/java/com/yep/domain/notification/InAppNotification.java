package com.yep.domain.notification;

import com.yep.domain.enums.NotificationType;
import com.yep.domain.user.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("IN_APP")
public class InAppNotification extends Notification {

    protected InAppNotification() {
    }

    public InAppNotification(User recipient, String message, String link) {
        super(recipient, message, link);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.IN_APP;
    }

    @Override
    public String send() {
        return "In-app notification stored for " + getRecipient().getEmail();
    }
}
