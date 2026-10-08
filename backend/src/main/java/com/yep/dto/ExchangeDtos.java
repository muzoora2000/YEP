package com.yep.dto;

import java.time.LocalDate;

import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.ExchangeStatus;
import com.yep.domain.enums.LevelOneTopic;
import com.yep.domain.enums.LevelTwoFocusArea;
import com.yep.domain.exchange.LevelOneExchange;
import com.yep.domain.exchange.LevelTwoExchange;
import com.yep.domain.exchange.VirtualExchange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Contracts for {@code /api/v1/exchanges}.
 * {@link ExchangeResponse#of(VirtualExchange)} is polymorphic: the level
 * specific columns are filled in depending on the concrete subclass.
 */
public final class ExchangeDtos {

    private ExchangeDtos() {
    }

    public record ExchangeResponse(
            Long id,
            String title,
            String description,
            ExchangeLevel level,
            String summaryLabel,
            LocalDate startDate,
            LocalDate endDate,
            ExchangeStatus status,
            LevelOneTopic topic,
            Integer maxParticipants,
            String sessionLink,
            Long facilitatorId,
            LevelTwoFocusArea focusArea,
            Integer maxTeams,
            Boolean applicationsOpen,
            int enrolledCount,
            int teamCount,
            boolean canEnroll) {

        public static ExchangeResponse of(VirtualExchange exchange) {
            LevelOneTopic topic = null;
            Integer maxParticipants = null;
            String sessionLink = null;
            Long facilitatorId = null;
            LevelTwoFocusArea focusArea = null;
            Integer maxTeams = null;
            Boolean applicationsOpen = null;
            int teamCount = 0;

            if (exchange instanceof LevelOneExchange levelOne) {
                topic = levelOne.getTopic();
                maxParticipants = levelOne.getMaxParticipants();
                sessionLink = levelOne.getSessionLink();
                facilitatorId = levelOne.getFacilitatorId();
            } else if (exchange instanceof LevelTwoExchange levelTwo) {
                focusArea = levelTwo.getFocusArea();
                maxTeams = levelTwo.getMaxTeams();
                applicationsOpen = levelTwo.isApplicationsOpen();
                teamCount = levelTwo.getTeams() != null ? levelTwo.getTeams().size() : 0;
            }

            return new ExchangeResponse(
                    exchange.getId(),
                    exchange.getTitle(),
                    exchange.getDescription(),
                    exchange.getLevel(),
                    exchange.getSummaryLabel(),
                    exchange.getStartDate(),
                    exchange.getEndDate(),
                    exchange.getStatus(),
                    topic,
                    maxParticipants,
                    sessionLink,
                    facilitatorId,
                    focusArea,
                    maxTeams,
                    applicationsOpen,
                    exchange.enrolledCount(),
                    teamCount,
                    exchange.isOpenForEnrollment());
        }
    }

    /**
     * Single create/update payload; the service validates that {@code topic} is
     * present for LEVEL_ONE and {@code focusArea} for LEVEL_TWO (polymorphic
     * validation on the requested level).
     */
    public record ExchangeRequest(
            @NotBlank(message = "title is required")
            @Size(max = 180)
            String title,

            @Size(max = 4000)
            String description,

            @NotNull(message = "level is required")
            ExchangeLevel level,

            @NotNull(message = "start date is required")
            LocalDate startDate,

            LocalDate endDate,

            ExchangeStatus status,

            LevelOneTopic topic,
            Integer maxParticipants,
            @Size(max = 500) String sessionLink,
            Long facilitatorId,

            LevelTwoFocusArea focusArea,
            Integer maxTeams,
            Boolean applicationsOpen) {
    }

    public record ExchangeStatusRequest(ExchangeStatus status) {
    }
}
