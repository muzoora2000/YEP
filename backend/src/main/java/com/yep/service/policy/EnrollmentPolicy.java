package com.yep.service.policy;

import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.user.Participant;

/**
 * Strategy for deciding whether a participant may join a virtual exchange.
 * One implementation exists per exchange level, so {@code EnrollmentService}
 * never has to branch on the concrete exchange subclass (Polymorphism).
 */
public interface EnrollmentPolicy {

    /** @throws com.yep.exception.ApiException subclass when the enrollment is not allowed */
    void check(VirtualExchange exchange, Participant participant);
}
