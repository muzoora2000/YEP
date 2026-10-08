package com.yep.domain.interfaces;

import com.yep.domain.enums.NotificationChannel;

/**
 * Anything the platform can deliver a notification to (users, teams, cohorts).
 * Implemented polymorphically by {@code User} and {@code Team} so the
 * notification dispatcher never needs to know the concrete recipient type.
 */
public interface Notifiable {

    /** Address the notification should be delivered to (e-mail, phone or in-app handle). */
    String getNotificationAddress();

    /** Preferred delivery channel. */
    NotificationChannel getPreferredChannel();

    /** Human readable label used in notification copy. */
    String getNotificationLabel();
}
