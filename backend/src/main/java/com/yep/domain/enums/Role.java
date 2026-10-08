package com.yep.domain.enums;

/**
 * Every actor that can hold an account on YEP.
 * Doubles as the {@code users.role} JPA discriminator column.
 */
public enum Role {
    PARTICIPANT,
    FACILITATOR,
    MENTOR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
