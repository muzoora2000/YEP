package com.yep.service;

import com.yep.domain.enums.NotificationChannel;
import com.yep.domain.interfaces.Notifiable;

/** Strategy contract for one delivery channel. */
public interface NotificationChannelHandler {

    NotificationChannel channel();

    void deliver(Notifiable target, String subject, String body);
}
