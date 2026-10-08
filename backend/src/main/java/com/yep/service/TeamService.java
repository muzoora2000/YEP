package com.yep.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.exchange.LevelTwoExchange;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.team.Team;
import com.yep.domain.team.TeamMember;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;
import com.yep.dto.TeamDtos;
import com.yep.exception.BadRequestException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.MentorRepository;
import com.yep.repository.ParticipantRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

/**
 * Team formation inside Level Two exchanges: participants self-select (the
 * creator becomes the team lead) or an admin/facilitator assigns members and
 * mentors on their behalf.
 */
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ParticipantRepository participantRepository;
    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;
    private final ExchangeService exchangeService;

    public TeamService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            ParticipantRepository participantRepository,
            MentorRepository mentorRepository,
            UserRepository userRepository,
            ExchangeService exchangeService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.participantRepository = participantRepository;
        this.mentorRepository = mentorRepository;
        this.userRepository = userRepository;
        this.exchangeService = exchangeService;
    }

    @Transactional
    public TeamDtos.TeamResponse create(TeamDtos.TeamRequest request) {
        AuthenticatedUser principal = SecurityUtils.current();

        VirtualExchange exchange = exchangeService.getEntity(request.levelTwoExchangeId());
        if (!(exchange instanceof LevelTwoExchange levelTwo)) {
            throw new BadRequestException("Teams can only be created inside a Level Two exchange");
        }
        if (teamRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("A team with this name already exists");
        }
        if (!levelTwo.hasRoomForTeam()) {
            throw new ConflictException("This programme already has the maximum number of teams");
        }

        Team team = new Team();
        team.setName(request.name().trim());
        team.setDescription(request.description());
        team.setLevelTwoExchange(levelTwo);

        if (principal.isAdmin() && request.mentorId() != null) {
            team.setMentor(loadMentor(request.mentorId()));
        }

        Team saved = teamRepository.save(team);

        if (principal.role() == com.yep.domain.enums.Role.PARTICIPANT) {
            Participant creator = participantRepository.findById(principal.id())
                    .orElseThrow(() -> new ForbiddenException("Only participants can join teams"));
            TeamMember membership = saved.addMember(creator, TeamMemberRole.LEAD);
            teamMemberRepository.save(membership);
        }

        return TeamDtos.TeamResponse.of(teamRepository.save(saved));
    }

    @Transactional(readOnly = true)
    public TeamDtos.TeamResponse get(Long id) {
        return TeamDtos.TeamResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public List<TeamDtos.TeamResponse> byExchange(Long exchangeId) {
        return teamRepository.findByLevelTwoExchangeId(exchangeId).stream()
                .map(TeamDtos.TeamResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeamDtos.TeamResponse> mine() {
        AuthenticatedUser principal = SecurityUtils.current();
        return teamRepository.findByParticipant(principal.id()).stream()
                .map(TeamDtos.TeamResponse::of)
                .toList();
    }

    /** Self-selection: an authenticated participant joins an open team. */
    @Transactional
    public TeamDtos.TeamResponse join(Long teamId) {
        AuthenticatedUser principal = SecurityUtils.current();
        Team team = find(teamId);
        Participant participant = participantRepository.findById(principal.id())
                .orElseThrow(() -> new ForbiddenException("Only participants can join teams"));
        return TeamDtos.TeamResponse.of(addMember(team, participant, TeamMemberRole.MEMBER, principal));
    }

    /** Admin / facilitator assigned membership. */
    @Transactional
    public TeamDtos.TeamResponse addMember(Long teamId, TeamDtos.AddMemberRequest request) {
        AuthenticatedUser principal = SecurityUtils.current();
        Team team = find(teamId);
        if (!principal.isAdmin() && !principal.isStaff() && !isTeamLead(team, principal)) {
            throw new ForbiddenException("Only team leads, facilitators or administrators can add members");
        }
        Participant participant = participantRepository.findById(request.participantId())
                .orElseThrow(() -> NotFoundException.resource("Participant", request.participantId()));
        return TeamDtos.TeamResponse.of(addMember(team, participant, request.memberRole(), principal));
    }

    @Transactional
    public TeamDtos.TeamResponse removeMember(Long teamId, Long participantId) {
        AuthenticatedUser principal = SecurityUtils.current();
        Team team = find(teamId);
        if (!principal.isAdmin() && !principal.isStaff() && !isTeamLead(team, principal)) {
            throw new ForbiddenException("Only team leads, facilitators or administrators can remove members");
        }
        teamMemberRepository.deleteByTeamIdAndParticipantId(teamId, participantId);
        teamMemberRepository.flush();
        return TeamDtos.TeamResponse.of(find(teamId));
    }

    @Transactional
    public TeamDtos.TeamResponse assignMentor(Long teamId, Long mentorId) {
        AuthenticatedUser principal = SecurityUtils.current();
        if (!principal.isAdmin() && !principal.isStaff()) {
            throw new ForbiddenException("Only facilitators or administrators can assign mentors");
        }
        Team team = find(teamId);
        team.setMentor(loadMentor(mentorId));
        return TeamDtos.TeamResponse.of(teamRepository.save(team));
    }

    @Transactional(readOnly = true)
    public boolean isMember(Long teamId, Long participantId) {
        return teamMemberRepository.existsByTeamIdAndParticipantId(teamId, participantId);
    }

    // ---------------------------------------------------------------------

    private Team addMember(Team team, Participant participant, TeamMemberRole role,
            AuthenticatedUser principal) {
        if (!team.getLevelTwoExchange().isOpenForEnrollment()
                && team.getLevelTwoExchange().getStatus() != com.yep.domain.enums.ExchangeStatus.ACTIVE) {
            throw new BadRequestException("This programme is not accepting new team members");
        }
        if (team.isMember(participant)) {
            throw new ConflictException("This participant is already on the team");
        }
        boolean alreadyInExchangeTeam = team.getLevelTwoExchange().getTeams() != null
                && team.getLevelTwoExchange().getTeams().stream().anyMatch(t -> t.isMember(participant));
        if (alreadyInExchangeTeam) {
            throw new ConflictException("This participant is already in a team for this programme");
        }

        TeamMember membership = team.addMember(participant, role);
        if (membership == null) {
            throw new ConflictException("This participant is already on the team");
        }
        TeamMember saved = teamMemberRepository.save(membership);
        team.getTeamMembers().add(saved);
        return teamRepository.save(team);
    }

    private boolean isTeamLead(Team team, AuthenticatedUser principal) {
        Participant lead = team.findTeamLead();
        return lead != null && lead.getId().equals(principal.id());
    }

    private Mentor loadMentor(Long mentorId) {
        User user = userRepository.findById(mentorId)
                .orElseThrow(() -> NotFoundException.resource("Mentor", mentorId));
        if (!(user instanceof Mentor mentor)) {
            throw new BadRequestException("The selected user is not a mentor");
        }
        return mentor;
    }

    private Team find(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> NotFoundException.resource("Team", id));
    }
}
