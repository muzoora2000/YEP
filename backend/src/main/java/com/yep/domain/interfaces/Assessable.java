package com.yep.domain.interfaces;

import java.util.List;

import com.yep.domain.enums.AssessmentTargetType;
import com.yep.domain.user.Participant;

/**
 * Anything that can receive a score and feedback from a facilitator or mentor.
 * Implemented by {@link Participant} and by {@code Team} so a single
 * {@code AssessmentService} can assess both targets polymorphically.
 */
public interface Assessable {

    /** Surrogate key of the assessed object (participant id or team id). */
    Long getId();

    /** Which concrete kind of target this is - drives which FK the assessment uses. */
    AssessmentTargetType getAssessmentTargetType();

    /** Label shown to the assessor (e.g. "Jane Doe" or "Team Kampala"). */
    String getAssessmentLabel();

    /** Participants that should receive the assessment outcome as a certificate. */
    List<Participant> getAssessmentRecipients();
}
