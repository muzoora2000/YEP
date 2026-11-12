package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.domain.enums.ActivityLevel;
import com.yep.dto.LearningDtos.ActivityCompletionSummary;
import com.yep.dto.LearningDtos.ActivityResponse;
import com.yep.dto.LearningDtos.EnrollmentResponse;
import com.yep.dto.LearningDtos.MyResultsResponse;
import com.yep.dto.PageResponse;
import com.yep.service.ActivityCompletionService;
import com.yep.service.ActivityService;

@RestController
@RequestMapping("/api/v1")
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityCompletionService completionService;

    public ActivityController(ActivityService activityService, ActivityCompletionService completionService) {
        this.activityService = activityService;
        this.completionService = completionService;
    }

    @GetMapping("/activities")
    public PageResponse<ActivityResponse> list(@RequestParam(required = false) ActivityLevel level,
            @RequestParam(required = false) String category, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return activityService.list(level, category, q, page, size);
    }

    @GetMapping("/activities/{activityId}")
    public ActivityResponse get(@PathVariable Long activityId) {
        return activityService.publicGet(activityId);
    }

    @PostMapping("/activities/{activityId}/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long activityId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.enroll(activityId));
    }

    @PostMapping("/activities/{activityId}/apply")
    public ResponseEntity<EnrollmentResponse> apply(@PathVariable Long activityId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.apply(activityId));
    }

    @PostMapping("/activities/{activityId}/complete")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ActivityCompletionSummary complete(@PathVariable Long activityId) {
        return completionService.complete(activityId);
    }

    @DeleteMapping("/enrollments/{enrollmentId}")
    public EnrollmentResponse cancel(@PathVariable Long enrollmentId) {
        return activityService.cancelEnrollment(enrollmentId);
    }

    @GetMapping("/me/enrollments")
    public List<EnrollmentResponse> myEnrollments() {
        return activityService.myEnrollments();
    }

    @GetMapping("/enrollments/me")
    public List<EnrollmentResponse> myEnrollmentsAlias() {
        return activityService.myEnrollments();
    }

    @GetMapping("/me/results")
    public List<MyResultsResponse> myResults() {
        return activityService.meResults();
    }

    // Spec-compatible aliases: the brief refers to learning activities as "virtual exchanges".
    @GetMapping("/exchanges")
    public PageResponse<ActivityResponse> listExchanges(@RequestParam(required = false) ActivityLevel level,
            @RequestParam(required = false) String category, @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        return activityService.list(level, category, q, page, size);
    }

    @GetMapping("/exchanges/{activityId}")
    public ActivityResponse getExchange(@PathVariable Long activityId) {
        return activityService.publicGet(activityId);
    }

    @PostMapping("/exchanges/{activityId}/enroll")
    public ResponseEntity<EnrollmentResponse> enrollExchange(@PathVariable Long activityId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.enroll(activityId));
    }
}
