package com.yep.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.yep.domain.enums.NotificationChannel;
import com.yep.domain.interfaces.Notifiable;

/**
 * Delivers a notification to any {@link Notifiable} target.
 *
 * <p>The delivery mechanism is chosen by the target's preferred
 * {@link NotificationChannel} and resolved through the injected
 * {@link NotificationChannelHandler} implementations - the dispatcher itself
 * never switches on concrete recipient types (Strategy + Polymorphism).</p>
 */
@Service
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final java.util.Map<NotificationChannel, NotificationChannelHandler> handlers;

    public NotificationDispatcher(java.util.List<NotificationChannelHandler> availableHandlers) {
        this.handlers = availableHandlers.stream()
                .collect(java.util.stream.Collectors.toMap(
                        NotificationChannelHandler::channel,
                        handler -> handler));
    }

    public void dispatch(Notifiable target, String subject, String body) {
        if (target == null) {
            return;
        }
        NotificationChannelHandler handler = handlers.get(target.getPreferredChannel());
        if (handler == null) {
            log.warn("No notification handler for channel {} (target {})", target.getPreferredChannel(),
                    target.getNotificationLabel());
            return;
        }
        handler.deliver(target, subject, body);
    }
}
