package com.yep.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.LearningDtos.ProgressResponse;
import com.yep.service.ProgressService;

@RestController
@RequestMapping("/api/v1")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @GetMapping("/progress/me")
    public List<ProgressResponse> myProgress() {
        return progressService.myProgress();
    }

    @GetMapping("/activities/{activityId}/progress")
    public ProgressResponse progress(@PathVariable Long activityId) {
        return progressService.progress(activityId);
    }

    @GetMapping("/facilitator/activities/{activityId}/progress/{youthId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ProgressResponse participantProgress(@PathVariable Long activityId, @PathVariable Long youthId) {
        return progressService.progressFor(activityId, youthId);
    }
}
