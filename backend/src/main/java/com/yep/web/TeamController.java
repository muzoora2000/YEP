package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.ProjectDtos.AddTeamMemberRequest;
import com.yep.dto.ProjectDtos.MentorAssignmentRequest;
import com.yep.dto.ProjectDtos.TeamRequest;
import com.yep.dto.ProjectDtos.TeamResponse;
import com.yep.service.TeamService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(request));
    }

    @GetMapping("/my")
    public List<TeamResponse> myTeams() {
        return teamService.myTeams();
    }

    @GetMapping("/mentored")
    public List<TeamResponse> mentoredTeams() {
        return teamService.mentorTeams();
    }

    @GetMapping("/activity/{activityId}")
    public List<TeamResponse> activityTeams(@PathVariable Long activityId) {
        return teamService.listForActivity(activityId);
    }

    @GetMapping("/{teamId}")
    public TeamResponse get(@PathVariable Long teamId) {
        return teamService.get(teamId);
    }

    @PostMapping("/{teamId}/members")
    public TeamResponse addMember(@PathVariable Long teamId, @RequestBody AddTeamMemberRequest request) {
        return teamService.addMember(teamId, request);
    }

    @DeleteMapping("/{teamId}/members/{youthId}")
    public TeamResponse removeMember(@PathVariable Long teamId, @PathVariable Long youthId) {
        return teamService.removeMember(teamId, youthId);
    }

    @PutMapping("/{teamId}/mentor")
    public TeamResponse assignMentor(@PathVariable Long teamId,
            @Valid @RequestBody MentorAssignmentRequest request) {
        return teamService.assignMentor(teamId, request);
    }

    @PutMapping("/{teamId}/lock")
    public TeamResponse lock(@PathVariable Long teamId) {
        return teamService.lock(teamId);
    }
}
