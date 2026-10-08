package com.yep.domain.message;

import java.time.OffsetDateTime;

import com.yep.domain.enums.MessageType;
import com.yep.domain.team.Team;
import com.yep.domain.user.User;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/** Message addressed to every member of a team. */
@Entity
@DiscriminatorValue("TEAM")
public class TeamMessage extends Message {

    public static TeamMessage toTeam(User sender, Team team, String subject, String body) {
        TeamMessage message = new TeamMessage();
        message.setSender(sender);
        message.setTeam(team);
        message.setSubject(subject);
        message.setBody(body);
        message.setSentAt(OffsetDateTime.now());
        message.setRead(false);
        return message;
    }

    public boolean concerns(Team other) {
        return getTeam() != null && getTeam().equals(other);
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.TEAM;
    }
}
