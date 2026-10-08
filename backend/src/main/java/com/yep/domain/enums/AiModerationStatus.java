package com.yep.domain.enums;

/** Outcome of the (AI assisted) moderation pass run on user generated content. */
public enum AiModerationStatus {
    NOT_SCREENED,
    PASSED,
    FLAGGED,
    OVERRIDDEN_BY_HUMAN
}
