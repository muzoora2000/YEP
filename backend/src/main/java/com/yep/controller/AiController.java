package com.yep.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.AiDtos;
import com.yep.service.AiAssistanceService;

import jakarta.validation.Valid;

/**
 * Responsible-AI assistant. Every response is labelled as AI generated and is
 * only ever a draft - publication always requires a human approval step.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiAssistanceService aiAssistanceService;

    public AiController(AiAssistanceService aiAssistanceService) {
        this.aiAssistanceService = aiAssistanceService;
    }

    @PostMapping("/suggest")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public AiDtos.SuggestionResponse suggest(@Valid @RequestBody AiDtos.SuggestionRequest request) {
        return aiAssistanceService.suggest(request.input(), request.kind());
    }

    @PostMapping("/moderate")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public AiDtos.ModerationResponse moderate(@Valid @RequestBody AiDtos.ModerationRequest request) {
        return aiAssistanceService.moderate(request.content());
    }
}
