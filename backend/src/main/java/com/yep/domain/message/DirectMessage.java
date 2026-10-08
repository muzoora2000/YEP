package com.yep.domain.message;

import java.time.OffsetDateTime;

import com.yep.domain.enums.MessageType;
import com.yep.domain.user.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** One-to-one conversation between two platform users. */
@Entity
@DiscriminatorValue("DIRECT")
public class DirectMessage extends Message {

    public static DirectMessage between(User sender, User recipient, String subject, String body) {
        DirectMessage message = new DirectMessage();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setSubject(subject);
        message.setBody(body);
        message.setSentAt(OffsetDateTime.now());
        message.setRead(false);
        return message;
    }

    public boolean isReplyTo(User user) {
        return getRecipient() != null && getRecipient().equals(user);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.DIRECT;
    }
}
