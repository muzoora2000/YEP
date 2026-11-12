package com.yep.domain.notification;

import com.yep.domain.enums.NotificationType;
import com.yep.domain.user.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SMS")
public class SmsNotification extends Notification {

    protected SmsNotification() {
    }

    public SmsNotification(User recipient, String message, String link) {
        super(recipient, message, link);
    }

    @Override
    public NotificationType getType() {
        return NotificationType.SMS;
    }

    @Override
    public String send() {
        return "SMS notification queued for " + getRecipient().getPhone();
    }
}
