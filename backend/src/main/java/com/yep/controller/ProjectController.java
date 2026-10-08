package com.yep.controller;

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

import com.yep.domain.enums.ProjectStatus;
import com.yep.dto.ProjectDtos;
import com.yep.service.ProjectService;

import jakarta.validation.Valid;

/** Impact Project lifecycle - {@code /api/v1/projects}. */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN', 'FACILITATOR')")
    public ResponseEntity<ProjectDtos.ProjectResponse> create(
            @RequestParam Long teamId,
            @Valid @RequestBody ProjectDtos.ProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(teamId, request));
    }

    @GetMapping("/{id}")
    public ProjectDtos.ProjectResponse get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN', 'FACILITATOR')")
    public ProjectDtos.ProjectResponse update(@PathVariable Long id,
            @Valid @RequestBody ProjectDtos.ProjectRequest request) {
        return projectService.update(id, request);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN', 'FACILITATOR')")
    public ProjectDtos.ProjectResponse submit(@PathVariable Long id) {
        return projectService.submit(id);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public ProjectDtos.ProjectResponse review(
            @PathVariable Long id,
            @RequestParam String decision,
            @RequestParam(required = false) String feedback) {
        return projectService.review(id, decision, feedback);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public ProjectDtos.ProjectResponse complete(@PathVariable Long id) {
        return projectService.complete(id);
    }

    @GetMapping
    public List<ProjectDtos.ProjectResponse> list(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) String mine) {
        if (Boolean.parseBoolean(mine)) {
            return projectService.mine();
        }
        if (status != null) {
            return projectService.byStatus(status);
        }
        return projectService.byStatus(ProjectStatus.SUBMITTED);
    }
}
