package com.yep.domain.exchange;

import java.time.LocalDate;

import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.LevelOneTopic;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Level One virtual exchange: a large group online learning / discussion session.
 */
@Entity
@DiscriminatorValue("LEVEL_ONE")
public class LevelOneExchange extends VirtualExchange {

    @Column(name = "topic", length = 50)
    @Enumerated(EnumType.STRING)
    private LevelOneTopic topic;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Column(name = "session_link", length = 500)
    private String sessionLink;

    /** Facilitator delivering this session (id only - keeps the users table free of extra FKs). */
    @Column(name = "facilitator_id")
    private Long facilitatorId;

    @Override
    public ExchangeLevel getLevel() {
        return ExchangeLevel.LEVEL_ONE;
    }

    @Override
    public String getSummaryLabel() {
        return "Level One - " + topic.name().replace('_', ' ');
    }

    @Override
    public boolean isAtCapacity() {
        if (maxParticipants == null || maxParticipants <= 0) {
            return false;
        }
        return enrolledCount() >= maxParticipants;
    }

    public boolean hasSeatsFor(int requested) {
        if (maxParticipants == null || maxParticipants <= 0) {
            return true;
        }
        return enrolledCount() + requested <= maxParticipants;
    }

    public boolean acceptsEnrollmentOn(LocalDate day) {
        return !isAtCapacity() && (getEndDate() == null || !day.isAfter(getEndDate()));
    }

    public LevelOneTopic getTopic() {
        return topic;
    }

    public void setTopic(LevelOneTopic topic) {
        this.topic = topic;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public String getSessionLink() {
        return sessionLink;
    }

    public void setSessionLink(String sessionLink) {
        this.sessionLink = sessionLink;
    }

    public Long getFacilitatorId() {
        return facilitatorId;
    }

    public void setFacilitatorId(Long facilitatorId) {
        this.facilitatorId = facilitatorId;
    }
}
