package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.ProjectDtos.MilestoneRequest;
import com.yep.dto.ProjectDtos.MilestoneResponse;
import com.yep.dto.ProjectDtos.MilestoneStatusRequest;
import com.yep.dto.ProjectDtos.ProjectRequest;
import com.yep.dto.ProjectDtos.ProjectResponse;
import com.yep.dto.ProjectDtos.ProjectUpdateRequest;
import com.yep.dto.ProjectDtos.ReviewRequest;
import com.yep.dto.ProjectDtos.ReviewResponse;
import com.yep.dto.ProjectDtos.SupportVisibilityRequest;
import com.yep.service.ProjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping("/my")
    public List<ProjectResponse> myProjects() {
        return projectService.myProjects();
    }

    @GetMapping("/mentored")
    public List<ProjectResponse> mentoredProjects() {
        return projectService.mentorProjects();
    }

    @GetMapping("/showcase")
    public List<ProjectResponse> showcase() {
        return projectService.showcase();
    }

    @GetMapping("/team/{teamId}")
    public ProjectResponse byTeam(@PathVariable Long teamId) {
        return projectService.getByTeam(teamId);
    }

    @GetMapping("/{projectId}")
    public ProjectResponse get(@PathVariable Long projectId) {
        return projectService.get(projectId);
    }

    @PutMapping("/{projectId}")
    public ProjectResponse update(@PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.update(projectId, request);
    }

    @PostMapping("/{projectId}/milestones")
    public ResponseEntity<MilestoneResponse> addMilestone(@PathVariable Long projectId,
            @Valid @RequestBody MilestoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.addMilestone(projectId, request));
    }

    @PutMapping("/milestones/{milestoneId}")
    public MilestoneResponse updateMilestone(@PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneStatusRequest request) {
        return projectService.updateMilestone(milestoneId, request);
    }

    @PostMapping("/{projectId}/submit")
    public ProjectResponse submit(@PathVariable Long projectId) {
        return projectService.submit(projectId);
    }

    @PostMapping("/{projectId}/reviews")
    public ResponseEntity<ReviewResponse> review(@PathVariable Long projectId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.review(projectId, request));
    }

    @GetMapping("/{projectId}/reviews")
    public List<ReviewResponse> reviews(@PathVariable Long projectId) {
        return projectService.reviews(projectId);
    }

    @PutMapping("/{projectId}/support-visibility")
    public ProjectResponse supportVisibility(@PathVariable Long projectId,
            @RequestBody SupportVisibilityRequest request) {
        return projectService.setSupportVisibility(projectId, request.openForSupport());
    }

    @PostMapping("/{projectId}/complete")
    public ProjectResponse complete(@PathVariable Long projectId) {
        return projectService.complete(projectId);
    }
}
