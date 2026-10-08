package com.yep.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.ExchangeStatus;
import com.yep.domain.enums.LevelOneTopic;
import com.yep.domain.enums.LevelTwoFocusArea;
import com.yep.dto.EnrollmentDtos;
import com.yep.dto.ExchangeDtos;
import com.yep.dto.PageResponse;
import com.yep.service.EnrollmentService;
import com.yep.service.ExchangeService;

import jakarta.validation.Valid;

/**
 * Browsing, filtering and administration of virtual exchanges
 * ({@code /api/v1/exchanges}). Reads are public; writes are role protected.
 */
@RestController
@RequestMapping("/api/v1/exchanges")
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final EnrollmentService enrollmentService;

    public ExchangeController(ExchangeService exchangeService, EnrollmentService enrollmentService) {
        this.exchangeService = exchangeService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    public PageResponse<ExchangeDtos.ExchangeResponse> search(
            @RequestParam(required = false) ExchangeLevel level,
            @RequestParam(required = false) LevelOneTopic topic,
            @RequestParam(required = false) LevelTwoFocusArea focusArea,
            @RequestParam(required = false) ExchangeStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return exchangeService.search(level, topic, focusArea, status, q, from, to, page, size);
    }

    @GetMapping("/{id}")
    public ExchangeDtos.ExchangeResponse get(@PathVariable Long id) {
        return exchangeService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('FACILITATOR', 'ADMIN')")
    public ResponseEntity<ExchangeDtos.ExchangeResponse> create(
            @Valid @RequestBody ExchangeDtos.ExchangeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'ADMIN')")
    public ExchangeDtos.ExchangeResponse update(@PathVariable Long id,
            @Valid @RequestBody ExchangeDtos.ExchangeRequest request) {
        return exchangeService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'ADMIN')")
    public ExchangeDtos.ExchangeResponse updateStatus(@PathVariable Long id,
            @RequestBody ExchangeDtos.ExchangeStatusRequest request) {
        return exchangeService.updateStatus(id, request.status());
    }

    /** Self-service enrollment (rules differ per level - see EnrollmentPolicy). */
    @PostMapping("/{id}/enroll")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public ResponseEntity<EnrollmentDtos.EnrollmentResponse> enroll(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(id));
    }

    @GetMapping("/{id}/enrollments")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public List<EnrollmentDtos.EnrollmentResponse> enrollments(@PathVariable Long id) {
        return enrollmentService.byExchange(id);
    }
}
