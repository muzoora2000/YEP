package com.yep.service.policy;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.yep.domain.enums.ExchangeStatus;
import com.yep.domain.exchange.LevelOneExchange;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.user.Participant;
import com.yep.exception.BadRequestException;
import com.yep.exception.ConflictException;

/**
 * Level One rule set: open status, seat capacity and enrolment deadline.
 */
@Component
public class LevelOneEnrollmentPolicy implements EnrollmentPolicy {

    @Override
    public void check(VirtualExchange exchange, Participant participant) {
        if (!(exchange instanceof LevelOneExchange levelOne)) {
            throw new BadRequestException("Level One enrollment rules do not apply to this exchange");
        }
        if (levelOne.getStatus() == ExchangeStatus.CANCELLED) {
            throw new BadRequestException("This session has been cancelled");
        }
        if (levelOne.getStatus() == ExchangeStatus.COMPLETED) {
            throw new BadRequestException("This session has already finished");
        }
        if (levelOne.isAtCapacity()) {
            throw new ConflictException("This session is full - all " + levelOne.getMaxParticipants()
                    + " seats are taken");
        }
        if (!levelOne.acceptsEnrollmentOn(LocalDate.now())) {
            throw new ConflictException("Enrollment for this session is closed");
        }
    }
}
