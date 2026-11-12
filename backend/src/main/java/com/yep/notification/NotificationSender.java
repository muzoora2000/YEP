package com.yep.notification;

/** Interface abstraction: all delivery channels implement this. */
public interface NotificationSender {
    void deliver(String recipient, String subject, String body);
    boolean isAvailable();
}
