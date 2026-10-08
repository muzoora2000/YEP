package com.yep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contracts for the responsible-AI assistant endpoints.
 * Every response is explicitly labelled as AI generated and is only ever a
 * draft - a human facilitator must review it before it can be published.
 */
public final class AiDtos {

    private AiDtos() {
    }

    public record SuggestionRequest(
            @NotBlank
            @Size(max = 4000)
            String input,

            String kind) {
    }

    public record SuggestionResponse(
            String draft,
            boolean aiGenerated,
            String label,
            String reviewInstructions) {

        public static SuggestionResponse of(String draft) {
            return new SuggestionResponse(
                    draft,
                    true,
                    "AI-GENERATED DRAFT - NOT PUBLISHED",
                    "A human facilitator must review, edit and approve this text before it is shared.");
        }
    }

    public record ModerationRequest(
            @NotBlank
            @Size(max = 4000)
            String content) {
    }

    public record ModerationResponse(
            com.yep.domain.enums.AiModerationStatus status,
            String reason,
            boolean humanReviewRequired,
            boolean aiGenerated) {
    }
}
