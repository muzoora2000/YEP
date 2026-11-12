package com.yep.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.enums.TeamStatus;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.project.Team;
import com.yep.domain.project.TeamMember;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.ProjectDtos.AddTeamMemberRequest;
import com.yep.dto.ProjectDtos.MentorAssignmentRequest;
import com.yep.dto.ProjectDtos.TeamMemberResponse;
import com.yep.dto.ProjectDtos.TeamRequest;
import com.yep.dto.ProjectDtos.TeamResponse;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.MentorRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ImpactProjectRepository projectRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;

    public TeamService(TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
            ImpactProjectRepository projectRepository, EnrollmentRepository enrollmentRepository,
            MentorRepository mentorRepository, UserRepository userRepository, ActivityService activityService,
            SecurityUtils securityUtils, AuditService auditService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.projectRepository = projectRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.mentorRepository = mentorRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        AuthenticatedUser actor = securityUtils.currentUser();
        LearningActivity activity = activityService.find(request.activityId());
        if (!activity.supportsTeamFormation()) {
            throw new BusinessRuleException("TEAM_FORMATION_UNAVAILABLE",
                    "Teams can only be formed for Level Two activities.");
        }
        if (!activity.isPublished()) {
            throw new BusinessRuleException("ACTIVITY_NOT_PUBLISHED",
                    "Teams can only be formed for a published activity.");
        }
        if (teamRepository.findFirstByActivityActivityIdAndTeamNameIgnoreCase(activity.getActivityId(),
                request.teamName().trim()).isPresent()) {
            throw new ConflictException("TEAM_NAME_TAKEN", "A team with that name already exists.");
        }

        User actorUser = actorUser(actor);
        Team team = new Team(activity, request.teamName().trim(), actorUser);
        if (actor.role() == Role.PARTICIPANT) {
            YouthParticipant youth = currentParticipant(actor);
            requireEnrolled(activity, youth);
            if (teamMemberRepository.existsByYouthUserIdAndTeamActivityActivityId(youth.getUserId(),
                    activity.getActivityId())) {
                throw new ConflictException("ALREADY_IN_TEAM", "You are already a member of a team in this activity.");
            }
            team.addMember(youth, TeamMemberRole.LEADER);
        } else if (actor.role() == Role.FACILITATOR || actor.role() == Role.ADMIN) {
            activityService.requireManage(activity);
            if (request.mentorId() != null) {
                team.setMentor(findMentor(request.mentorId()));
            }
        } else {
            throw new ForbiddenException("FORBIDDEN", "Only participants, facilitators and administrators create teams.");
        }
        team.refreshStatus(activity.getMaxTeamSize());
        team = teamRepository.save(team);
        auditService.record(actorUser, "TEAM_CREATED", "Team", team.getTeamId(), team.getTeamName());
        return toResponse(team);
    }

    @Transactional(readOnly = true)
    public TeamResponse get(Long teamId) {
        Team team = find(teamId);
        requireView(team);
        return toResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> myTeams() {
        YouthParticipant youth = currentParticipant(securityUtils.currentUser());
        return teamMemberRepository.findByYouthUserId(youth.getUserId()).stream()
                .map(TeamMember::getTeam)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> mentorTeams() {
        AuthenticatedUser actor = securityUtils.currentUser();
        if (actor.role() != Role.MENTOR) {
            throw new ForbiddenException("MENTOR_ONLY", "Mentor account required.");
        }
        return teamRepository.findByMentorUserId(actor.userId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listForActivity(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        return teamRepository.findByActivityActivityId(activityId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public TeamResponse addMember(Long teamId, AddTeamMemberRequest request) {
        Team team = find(teamId);
        AuthenticatedUser actor = securityUtils.currentUser();
        YouthParticipant youth;
        if (request.youthId() == null || request.youthId().equals(actor.userId())) {
            youth = currentParticipant(actor);
            if (!youth.getUserId().equals(actor.userId())) {
                throw new ForbiddenException("FORBIDDEN", "You can only add yourself to a team.");
            }
        } else {
            requireManage(team);
            User user = userRepository.findById(request.youthId())
                    .orElseThrow(() -> new NotFoundException("PARTICIPANT_NOT_FOUND", "Participant not found."));
            if (!(user instanceof YouthParticipant participant)) {
                throw new BusinessRuleException("PARTICIPANT_ONLY", "Only youth participants can join teams.");
            }
            youth = participant;
        }
        requireEnrolled(team.getActivity(), youth);
        if (team.getStatus() == TeamStatus.LOCKED) {
            throw new BusinessRuleException("TEAM_LOCKED", "This team is locked and cannot accept new members.");
        }
        if (team.memberCount() >= team.getActivity().getMaxTeamSize()) {
            throw new BusinessRuleException("TEAM_FULL", "This team has reached its maximum size.");
        }
        if (teamMemberRepository.existsByYouthUserIdAndTeamActivityActivityId(youth.getUserId(),
                team.getActivity().getActivityId())) {
            throw new ConflictException("ALREADY_IN_TEAM", "That participant is already in a team for this activity.");
        }
        team.addMember(youth, TeamMemberRole.MEMBER);
        team.refreshStatus(team.getActivity().getMaxTeamSize());
        teamRepository.save(team);
        auditService.record(actorUser(actor), "TEAM_MEMBER_ADDED", "Team", teamId, youth.getName());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse removeMember(Long teamId, Long youthId) {
        Team team = find(teamId);
        AuthenticatedUser actor = securityUtils.currentUser();
        if (!actor.userId().equals(youthId)) {
            requireManage(team);
        }
        if (team.getStatus() == TeamStatus.LOCKED) {
            throw new BusinessRuleException("TEAM_LOCKED", "This team is locked and cannot change members.");
        }
        YouthParticipant target = team.getMembers().stream()
                .filter(member -> member.getYouth().getUserId().equals(youthId))
                .map(TeamMember::getYouth)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("TEAM_MEMBER_NOT_FOUND", "Team member not found."));
        boolean wasLeader = team.isLeader(target);
        team.removeMember(target);
        if (wasLeader && !team.getMembers().isEmpty()) {
            team.getMembers().get(0).assignRole(TeamMemberRole.LEADER);
        }
        team.refreshStatus(team.getActivity().getMaxTeamSize());
        teamRepository.save(team);
        auditService.record(actorUser(actor), "TEAM_MEMBER_REMOVED", "Team", teamId, target.getName());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse assignMentor(Long teamId, MentorAssignmentRequest request) {
        Team team = find(teamId);
        requireManage(team);
        team.setMentor(findMentor(request.mentorId()));
        teamRepository.save(team);
        auditService.record(actorUser(securityUtils.currentUser()), "TEAM_MENTOR_ASSIGNED", "Team", teamId,
                team.getMentor().getName());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse lock(Long teamId) {
        Team team = find(teamId);
        requireManage(team);
        if (team.memberCount() < team.getActivity().getMinTeamSize()) {
            throw new BusinessRuleException("TEAM_TOO_SMALL",
                    "The team must reach the minimum size before it can be locked.");
        }
        team.setStatus(TeamStatus.LOCKED);
        teamRepository.save(team);
        auditService.record(actorUser(securityUtils.currentUser()), "TEAM_LOCKED", "Team", teamId, team.getTeamName());
        return toResponse(team);
    }

    @Transactional(readOnly = true)
    public Team find(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new NotFoundException("TEAM_NOT_FOUND", "Team not found."));
    }

    public void requireView(Team team) {
        AuthenticatedUser actor = securityUtils.currentUser();
        if (actor.role() == Role.ADMIN) {
            return;
        }
        if (actor.role() == Role.FACILITATOR
                && team.getActivity().getFacilitator().getUserId().equals(actor.userId())) {
            return;
        }
        if (team.getMentor() != null && team.getMentor().getUserId().equals(actor.userId())) {
            return;
        }
        if (team.getMembers().stream().anyMatch(member -> member.getYouth().getUserId().equals(actor.userId()))) {
            return;
        }
        throw new ForbiddenException("FORBIDDEN", "You cannot view this team.");
    }

    public void requireManage(Team team) {
        AuthenticatedUser actor = securityUtils.currentUser();
        if (actor.role() == Role.ADMIN) {
            return;
        }
        if (actor.role() == Role.FACILITATOR
                && team.getActivity().getFacilitator().getUserId().equals(actor.userId())) {
            return;
        }
        if (team.getMentor() != null && team.getMentor().getUserId().equals(actor.userId())) {
            return;
        }
        throw new ForbiddenException("FORBIDDEN", "Only the assigned facilitator, mentor or an administrator can do that.");
    }

    public TeamResponse toResponse(Team team) {
        ImpactProject project = projectRepository.findByTeamTeamId(team.getTeamId()).orElse(null);
        List<TeamMemberResponse> members = team.getMembers().stream()
                .map(member -> new TeamMemberResponse(member.getMemberId(), member.getYouth().getUserId(),
                        member.getYouth().getName(), member.getYouth().getEmail(), member.getRole()))
                .toList();
        return new TeamResponse(team.getTeamId(), team.getActivity().getActivityId(),
                team.getActivity().getTitle(), team.getTeamName(),
                team.getMentor() == null ? null : team.getMentor().getUserId(),
                team.getMentor() == null ? null : team.getMentor().getName(),
                team.getCreatedBy() == null ? null : team.getCreatedBy().getUserId(),
                team.getCreatedBy() == null ? null : team.getCreatedBy().getName(),
                team.getStatus(), team.memberCount(), team.getActivity().getMinTeamSize(),
                team.getActivity().getMaxTeamSize(), members,
                project == null ? null : project.getProjectId(), project == null ? null : project.getTitle());
    }

    private void requireEnrolled(LearningActivity activity, YouthParticipant youth) {
        var enrollment = enrollmentRepository.findByYouthUserIdAndActivityActivityId(youth.getUserId(),
                activity.getActivityId())
                .orElseThrow(() -> new ForbiddenException("ENROLLMENT_REQUIRED",
                        "Enroll in this activity before joining a team."));
        if (enrollment.getStatus() != EnrollmentStatus.ENROLLED
                && enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
            throw new ForbiddenException("ENROLLMENT_REQUIRED", "An active enrollment is required to join a team.");
        }
    }

    private YouthParticipant currentParticipant(AuthenticatedUser actor) {
        User user = userRepository.findById(actor.userId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
        if (!(user instanceof YouthParticipant participant)) {
            throw new ForbiddenException("PARTICIPANT_ONLY", "Only youth participants can do that.");
        }
        return participant;
    }

    private Mentor findMentor(Long mentorId) {
        return mentorRepository.findById(mentorId)
                .orElseThrow(() -> new NotFoundException("MENTOR_NOT_FOUND", "Mentor not found."));
    }

    private User actorUser(AuthenticatedUser actor) {
        return userRepository.findById(actor.userId()).orElse(null);
    }
}
