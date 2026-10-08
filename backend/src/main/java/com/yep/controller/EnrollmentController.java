package com.yep.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.EnrollmentDtos;
import com.yep.service.EnrollmentService;

/** Enrollment lifecycle - {@code /api/v1/enrollments}. */
@RestController
@RequestMapping("/api/v1/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/mine")
    public List<EnrollmentDtos.EnrollmentResponse> mine() {
        return enrollmentService.mine();
    }

    @PostMapping("/{id}/withdraw")
    public EnrollmentDtos.EnrollmentResponse withdraw(@PathVariable Long id) {
        return enrollmentService.withdraw(id);
    }

    /** Facilitator closes the session and triggers certificate issuance. */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public EnrollmentDtos.EnrollmentResponse complete(@PathVariable Long id) {
        return enrollmentService.complete(id);
    }
}
