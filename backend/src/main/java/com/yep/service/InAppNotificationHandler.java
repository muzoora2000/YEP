package com.yep.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.NotificationChannel;
import com.yep.domain.interfaces.Notifiable;
import com.yep.domain.message.SystemMessage;
import com.yep.domain.message.TeamMessage;
import com.yep.domain.team.Team;
import com.yep.domain.user.User;
import com.yep.repository.MessageRepository;

/**
 * In-app delivery: writes a {@code SYSTEM} message for a single user, or a
 * {@code TEAM} message for a whole team roster.
 */
@Component
public class InAppNotificationHandler implements NotificationChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationHandler.class);

    private final MessageRepository messageRepository;

    public InAppNotificationHandler(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    @Transactional
    public void deliver(Notifiable target, String subject, String body) {
        if (target instanceof User user) {
            messageRepository.save(SystemMessage.forRecipient(user, subject, body));
        } else if (target instanceof Team team) {
            TeamMessage message = new TeamMessage();
            message.setTeam(team);
            message.setSubject(subject);
            message.setBody(body);
            message.setSentAt(java.time.OffsetDateTime.now());
            message.setRead(false);
            messageRepository.save(message);
        } else {
            log.warn("In-app channel cannot deliver to {}", target.getClass().getSimpleName());
        }
    }
}
