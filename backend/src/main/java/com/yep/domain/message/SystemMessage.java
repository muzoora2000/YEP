package com.yep.domain.message;

import java.time.OffsetDateTime;

import com.yep.domain.enums.MessageType;
import com.yep.domain.user.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Platform generated notification (system sender). Created by the notification
 * dispatcher for every {@code Notifiable} recipient.
 */
@Entity
@DiscriminatorValue("SYSTEM")
public class SystemMessage extends Message {

    public static SystemMessage forRecipient(User recipient, String subject, String body) {
        SystemMessage message = new SystemMessage();
        message.setRecipient(recipient);
        message.setSubject(subject);
        message.setBody(body);
        message.setSentAt(OffsetDateTime.now());
        message.setRead(false);
        return message;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.SYSTEM;
    }
}
