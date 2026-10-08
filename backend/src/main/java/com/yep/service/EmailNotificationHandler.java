package com.yep.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.NotificationChannel;
import com.yep.domain.interfaces.Notifiable;
import com.yep.domain.message.SystemMessage;
import com.yep.domain.user.User;
import com.yep.repository.MessageRepository;

/**
 * E-mail delivery channel.
 *
 * <p>This build ships without an SMTP dependency (the free Render tier has no
 * outbound mail service attached), so the handler logs the outbound mail and
 * stores the same content as an in-app {@code SYSTEM} message, which keeps a
 * complete, auditable outbox. Swapping in JavaMail/SendGrid only means
 * replacing this single class.</p>
 */
@Component
public class EmailNotificationHandler implements NotificationChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationHandler.class);

    private final MessageRepository messageRepository;

    public EmailNotificationHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    @Transactional
    public void deliver(Notifiable target, String subject, String body) {
        log.info("[EMAIL OUTBOX] to={} subject=\"{}\"", target.getNotificationAddress(), subject);
        if (target instanceof User user) {
            messageRepository.save(SystemMessage.forRecipient(user, subject, body));
        }
    }
}
