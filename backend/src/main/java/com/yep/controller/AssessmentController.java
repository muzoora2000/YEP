package com.yep.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.AssessmentDtos;
import com.yep.service.AssessmentService;

import jakarta.validation.Valid;

/** Assessment of participants and teams - {@code /api/v1/assessments}. */
@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public ResponseEntity<AssessmentDtos.AssessmentResponse> create(
            @Valid @RequestBody AssessmentDtos.AssessmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.create(request));
    }

    @GetMapping("/{id}")
    public AssessmentDtos.AssessmentResponse get(@PathVariable Long id) {
        return assessmentService.get(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public List<AssessmentDtos.AssessmentResponse> list(
            @RequestParam(required = false) Long participantId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) Boolean mine) {
        if (Boolean.TRUE.equals(mine)) {
            return assessmentService.givenByMe();
        }
        if (participantId != null) {
            return assessmentService.forParticipant(participantId);
        }
        if (teamId != null) {
            return assessmentService.forTeam(teamId);
        }
        return assessmentService.givenByMe();
    }

    /** Everything written about the current user (participant view). */
    @GetMapping("/about-me")
    public List<AssessmentDtos.AssessmentResponse> aboutMe() {
        return assessmentService.aboutMe();
    }
}
