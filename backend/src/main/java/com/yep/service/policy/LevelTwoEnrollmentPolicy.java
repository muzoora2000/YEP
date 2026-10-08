package com.yep.service.policy;

import org.springframework.stereotype.Component;

import com.yep.domain.enums.ExchangeStatus;
import com.yep.domain.exchange.LevelTwoExchange;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.user.Participant;
import com.yep.exception.BadRequestException;
import com.yep.exception.ConflictException;

/**
 * Level Two rule set: applications must be open, the cohort must not be
 * cancelled and the participant must not already be part of a team here.
 */
@Component
public class LevelTwoEnrollmentPolicy implements EnrollmentPolicy {

    @Override
    public void check(VirtualExchange exchange, Participant participant) {
        if (!(exchange instanceof LevelTwoExchange levelTwo)) {
            throw new BadRequestException("Level Two enrollment rules do not apply to this exchange");
        }
        if (levelTwo.getStatus() == ExchangeStatus.CANCELLED) {
            throw new BadRequestException("This programme has been cancelled");
        }
        if (!levelTwo.isApplicationsOpen()) {
            throw new ConflictException("Applications for this programme are currently closed");
        }
        if (levelTwo.getTeams() != null && levelTwo.getTeams().stream()
                .anyMatch(team -> team.isMember(participant))) {
            throw new ConflictException("You are already part of a team in this programme");
        }
    }
}
