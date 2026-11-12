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
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.LearningDtos.AssessmentResponse;
import com.yep.dto.LearningDtos.GradeRequest;
import com.yep.dto.LearningDtos.SubmissionRequest;
import com.yep.dto.LearningDtos.TaskRequest;
import com.yep.dto.LearningDtos.TaskResponse;
import com.yep.service.AssessmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/activities/{activityId}/tasks")
    public List<TaskResponse> tasks(@PathVariable Long activityId) {
        return assessmentService.listTasks(activityId);
    }

    @PostMapping("/activities/{activityId}/tasks")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<TaskResponse> createTask(@PathVariable Long activityId,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.createTask(activityId, request));
    }

    @PutMapping("/tasks/{taskId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public TaskResponse updateTask(@PathVariable Long taskId, @Valid @RequestBody TaskRequest request) {
        return assessmentService.updateTask(taskId, request);
    }

    @PostMapping("/tasks/{taskId}/submit")
    public ResponseEntity<AssessmentResponse> submit(@PathVariable Long taskId,
            @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.submit(taskId, request));
    }

    @GetMapping("/tasks/{taskId}/submissions")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public List<AssessmentResponse> submissionsForTask(@PathVariable Long taskId) {
        return assessmentService.submissionsForTask(taskId);
    }

    @GetMapping("/assessments/me")
    public List<AssessmentResponse> mySubmissions() {
        return assessmentService.mySubmissions();
    }

    @PutMapping("/assessments/{assessmentId}/grade")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public AssessmentResponse grade(@PathVariable Long assessmentId, @Valid @RequestBody GradeRequest request) {
        return assessmentService.grade(assessmentId, request);
    }
}
