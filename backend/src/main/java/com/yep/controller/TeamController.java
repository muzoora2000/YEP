package com.yep.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.PageResponse;
import com.yep.dto.ProjectDtos;
import com.yep.dto.TeamDtos;
import com.yep.service.ProjectService;
import com.yep.service.TeamService;

import jakarta.validation.Valid;

/** Team formation and membership - {@code /api/v1/teams}. */
@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;
    private final ProjectService projectService;

    public TeamController(TeamService teamService, ProjectService projectService) {
        this.teamService = teamService;
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'ADMIN', 'FACILITATOR')")
    public ResponseEntity<TeamDtos.TeamResponse> create(@Valid @RequestBody TeamDtos.TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(request));
    }

    @GetMapping("/{id}")
    public TeamDtos.TeamResponse get(@PathVariable Long id) {
        return teamService.get(id);
    }

    @GetMapping
    public List<TeamDtos.TeamResponse> list(@RequestParam(required = false) Long exchangeId) {
        if (exchangeId == null) {
            return teamService.mine();
        }
        return teamService.byExchange(exchangeId);
    }

    /** Self-selection: join an existing team. */
    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('PARTICIPANT')")
    public TeamDtos.TeamResponse join(@PathVariable Long id) {
        return teamService.join(id);
    }

    @PostMapping("/{id}/members")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'FACILITATOR', 'MENTOR', 'ADMIN')")
    public TeamDtos.TeamResponse addMember(@PathVariable Long id,
            @Valid @RequestBody TeamDtos.AddMemberRequest request) {
        return teamService.addMember(id, request);
    }

    @DeleteMapping("/{id}/members/{participantId}")
    @PreAuthorize("hasAnyRole('PARTICIPANT', 'FACILITATOR', 'MENTOR', 'ADMIN')")
    public TeamDtos.TeamResponse removeMember(@PathVariable Long id, @PathVariable Long participantId) {
        return teamService.removeMember(id, participantId);
    }

    @PostMapping("/{id}/mentor")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'ADMIN')")
    public TeamDtos.TeamResponse assignMentor(@PathVariable Long id,
            @Valid @RequestBody TeamDtos.AssignMentorRequest request) {
        return teamService.assignMentor(id, request.mentorId());
    }

    /** The impact project of one team (convenience for the team detail page). */
    @GetMapping("/{id}/project")
    public ProjectDtos.ProjectResponse project(@PathVariable Long id) {
        return projectService.forTeam(id);
    }
}
