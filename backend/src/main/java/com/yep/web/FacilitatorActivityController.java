package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.domain.enums.ActivityStatus;
import com.yep.dto.LearningDtos.ActivityResponse;
import com.yep.dto.LearningDtos.CreateActivityRequest;
import com.yep.dto.LearningDtos.EnrollmentDecisionRequest;
import com.yep.dto.LearningDtos.EnrollmentResponse;
import com.yep.dto.LearningDtos.ParticipantRow;
import com.yep.dto.LearningDtos.UpdateActivityRequest;
import com.yep.dto.PageResponse;
import com.yep.service.ActivityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class FacilitatorActivityController {

    private final ActivityService activityService;

    public FacilitatorActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/facilitator/activities")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public PageResponse<ActivityResponse> listMine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return activityService.listMine(page, size);
    }

    @PostMapping("/activities")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<ActivityResponse> create(@Valid @RequestBody CreateActivityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.create(request));
    }

    @GetMapping("/facilitator/activities/{activityId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ActivityResponse get(@PathVariable Long activityId) {
        return activityService.manageGet(activityId);
    }

    @PutMapping("/activities/{activityId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ActivityResponse update(@PathVariable Long activityId,
            @Valid @RequestBody UpdateActivityRequest request) {
        return activityService.update(activityId, request);
    }

    @PutMapping("/activities/{activityId}/status")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ActivityResponse changeStatus(@PathVariable Long activityId, @RequestBody StatusRequest request) {
        return activityService.changeStatus(activityId, request.status());
    }

    @GetMapping("/activities/{activityId}/participants")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public List<ParticipantRow> participants(@PathVariable Long activityId) {
        return activityService.participants(activityId);
    }

    @PutMapping("/enrollments/{enrollmentId}/decision")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public EnrollmentResponse decide(@PathVariable Long enrollmentId,
            @RequestBody EnrollmentDecisionRequest request) {
        return activityService.decide(enrollmentId, request);
    }

    public record StatusRequest(ActivityStatus status) {
    }
}
