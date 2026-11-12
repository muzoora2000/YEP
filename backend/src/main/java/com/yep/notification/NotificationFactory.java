package com.yep.notification;

import com.yep.domain.enums.NotificationType;
import com.yep.domain.notification.EmailNotification;
import com.yep.domain.notification.InAppNotification;
import com.yep.domain.notification.Notification;
import com.yep.domain.notification.SmsNotification;
import com.yep.domain.user.User;

/** Factory Pattern: creates the right Notification subclass by type. */
public final class NotificationFactory {

    private NotificationFactory() {}

    public static Notification create(NotificationType type, User recipient, String message, String link) {
        return switch (type) {
            case EMAIL -> new EmailNotification(recipient, message, link);
            case SMS -> new SmsNotification(recipient, message, link);
            default -> new InAppNotification(recipient, message, link);
        };
    }
}
