package com.yep.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.assessment.Assessment;
import com.yep.domain.interfaces.Assessable;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;
import com.yep.dto.AssessmentDtos;
import com.yep.exception.BadRequestException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.AssessmentRepository;
import com.yep.repository.ParticipantRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

/**
 * Assessment of participants and teams.
 *
 * <p>The target is resolved through the {@link Assessable} interface and bound
 * by the polymorphic {@link Assessment#of} factory, so one code path handles
 * both target kinds.</p>
 */
@Service
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final ParticipantRepository participantRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final NotificationDispatcher notificationDispatcher;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            ParticipantRepository participantRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            NotificationDispatcher notificationDispatcher) {
        this.assessmentRepository = assessmentRepository;
        this.participantRepository = participantRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public AssessmentDtos.AssessmentResponse create(AssessmentDtos.AssessmentRequest request) {
        Assessable target = resolve(request);
        User assessor = loadAssessor(SecurityUtils.current());

        Assessment assessment = Assessment.of(target, assessor, request.criteria(), request.score(),
                request.feedback());
        Assessment saved = assessmentRepository.save(assessment);

        for (Participant recipient : saved.affectedParticipants()) {
            notificationDispatcher.dispatch(recipient,
                    "New assessment available",
                    "You have been assessed on: " + saved.getCriteria() + " (score " + saved.getScore() + "/100)");
        }

        return AssessmentDtos.AssessmentResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public AssessmentDtos.AssessmentResponse get(Long id) {
        return AssessmentDtos.AssessmentResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public List<AssessmentDtos.AssessmentResponse> forParticipant(Long participantId) {
        return assessmentRepository.findByParticipantTargetId(participantId).stream()
                .map(AssessmentDtos.AssessmentResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentDtos.AssessmentResponse> forTeam(Long teamId) {
        return assessmentRepository.findByTeamTargetId(teamId).stream()
                .map(AssessmentDtos.AssessmentResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentDtos.AssessmentResponse> givenByMe() {
        return assessmentRepository.findByAssessorId(SecurityUtils.currentUserId()).stream()
                .map(AssessmentDtos.AssessmentResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AssessmentDtos.AssessmentResponse> aboutMe() {
        Long userId = SecurityUtils.currentUserId();
        List<Assessment> results = new ArrayList<>(assessmentRepository.findByParticipantTargetId(userId));
        teamRepository.findByParticipant(userId)
                .forEach(team -> results.addAll(assessmentRepository.findByTeamTargetId(team.getId())));
        return results.stream().map(AssessmentDtos.AssessmentResponse::of).toList();
    }

    // ---------------------------------------------------------------------

    private Assessable resolve(AssessmentDtos.AssessmentRequest request) {
        if (request.targetType() == null) {
            throw new BadRequestException("target type is required");
        }
        return switch (request.targetType()) {
            case PARTICIPANT -> {
                if (request.participantId() == null) {
                    throw new BadRequestException("participantId is required for a participant assessment");
                }
                yield participantRepository.findById(request.participantId())
                        .orElseThrow(() -> NotFoundException.resource("Participant", request.participantId()));
            }
            case TEAM -> {
                if (request.teamId() == null) {
                    throw new BadRequestException("teamId is required for a team assessment");
                }
                yield teamRepository.findById(request.teamId())
                        .orElseThrow(() -> NotFoundException.resource("Team", request.teamId()));
            }
        };
    }

    private User loadAssessor(AuthenticatedUser principal) {
        if (!principal.isStaff()) {
            throw new ForbiddenException("Only facilitators, mentors or administrators can assess");
        }
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new ForbiddenException("Only facilitators, mentors or administrators can assess"));
    }

    private Assessment find(Long id) {
        return assessmentRepository.findById(id).orElseThrow(() -> NotFoundException.resource("Assessment", id));
    }
}
