package com.yep.service;

import org.springframework.stereotype.Service;

/**
 * Responsible-AI helpers.
 *
 * <p>Everything this service returns is <b>explicitly labelled as AI generated</b>
 * and is only ever a draft: a human facilitator must review it before it can be
 * published (see {@code Announcement.requiresHumanApproval}). The implementation
 * is deterministic and offline so the platform never leaks user content to a
 * third party model.</p>
 */
@Service
public class AiAssistanceService {

    private static final String[] BLOCKED = { "slur", "idiot", "kill", "hate" };

    /** Returns a clearly labelled writing draft based on the facilitator's prompt. */
    public com.yep.dto.AiDtos.SuggestionResponse suggest(String input, String kind) {
        String topic = input == null ? "" : input.strip();
        String scope = switch (kind == null ? "general" : kind.toLowerCase()) {
            case "announcement" -> "Announcement draft";
            case "feedback" -> "Assessment feedback draft";
            case "project" -> "Impact project draft";
            default -> "Session prompt draft";
        };

        String draft = scope + "\n\n"
                + "Working with: " + (topic.isEmpty() ? "(no prompt supplied)" : topic) + "\n\n"
                + "Suggested outline:\n"
                + "1. Context - why this matters for young people in East Africa.\n"
                + "2. Key question for the group - one open, discussion friendly question.\n"
                + "3. Two concrete examples from everyday life in Uganda.\n"
                + "4. Call to action - what participants should do before the next session.\n\n"
                + "[AI-GENERATED DRAFT - requires human review before publishing]";

        return com.yep.dto.AiDtos.SuggestionResponse.of(draft);
    }

    /** Lightweight, explainable content screen. Always reviewable by a human. */
    public com.yep.dto.AiDtos.ModerationResponse moderate(String content) {
        if (content == null || content.isBlank()) {
            return new com.yep.dto.AiDtos.ModerationResponse(
                    com.yep.domain.enums.AiModerationStatus.PASSED, "Empty content", false, true);
        }
        String lowered = content.toLowerCase();
        for (String word : BLOCKED) {
            if (lowered.contains(word)) {
                return new com.yep.dto.AiDtos.ModerationResponse(
                        com.yep.domain.enums.AiModerationStatus.FLAGGED,
                        "Possible harmful language detected (" + word + ")",
                        true,
                        true);
            }
        }
        return new com.yep.dto.AiDtos.ModerationResponse(
                com.yep.domain.enums.AiModerationStatus.PASSED,
                "No issues detected by the automated screen",
                true,
                true);
    }

    /** Returns a flag reason when moderation fails, {@code null} when the content passes. */
    public String flagReason(String content) {
        com.yep.dto.AiDtos.ModerationResponse result = moderate(content);
        return result.status() == com.yep.domain.enums.AiModerationStatus.FLAGGED ? result.reason() : null;
    }
}
