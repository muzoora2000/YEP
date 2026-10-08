package com.yep.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.interfaces.Certifiable;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.team.Team;
import com.yep.domain.user.User;
import com.yep.dto.CertificateDtos;
import com.yep.dto.ProjectDtos;
import com.yep.exception.BadRequestException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

/**
 * Impact Project lifecycle: draft -&gt; submitted -&gt; under review -&gt;
 * approved -&gt; completed.
 *
 * <p>Completion hands the project to {@link CertificateService} through the
 * {@link Certifiable} interface, which then issues one certificate per team
 * member - the service never down-casts to reach the team roster.</p>
 */
@Service
public class ProjectService {

    private final ImpactProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final CertificateService certificateService;
    private final AiAssistanceService aiAssistanceService;
    private final NotificationDispatcher notificationDispatcher;

    public ProjectService(
            ImpactProjectRepository projectRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository,
            CertificateService certificateService,
            AiAssistanceService aiAssistanceService,
            NotificationDispatcher notificationDispatcher) {
        this.projectRepository = projectRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.certificateService = certificateService;
        this.aiAssistanceService = aiAssistanceService;
        this.notificationDispatcher = notificationDispatcher;
    }

    @Transactional
    public ProjectDtos.ProjectResponse create(Long teamId, ProjectDtos.ProjectRequest request) {
        Team team = findTeam(teamId);
        requireMemberOrStaff(team);

        if (projectRepository.existsByTeamId(teamId)) {
            throw new ConflictException("This team already has an impact project");
        }

        ImpactProject project = ImpactProject.draftFor(team, request.title().trim());
        applyContent(project, request);
        ImpactProject saved = projectRepository.save(project);
        team.setImpactProject(saved);
        teamRepository.save(team);
        return ProjectDtos.ProjectResponse.of(saved);
    }

    @Transactional
    public ProjectDtos.ProjectResponse update(Long id, ProjectDtos.ProjectRequest request) {
        ImpactProject project = find(id);
        requireMemberOrStaff(project.getTeam());
        if (project.getStatus() != ProjectStatus.DRAFT) {
            throw new BadRequestException("Only a draft project can be edited");
        }
        project.setTitle(request.title().trim());
        applyContent(project, request);
        return ProjectDtos.ProjectResponse.of(projectRepository.save(project));
    }

    @Transactional
    public ProjectDtos.ProjectResponse submit(Long id) {
        ImpactProject project = find(id);
        requireMemberOrStaff(project.getTeam());
        if (!project.canBeSubmitted()) {
            throw new BadRequestException("This project cannot be submitted in its current state ("
                    + project.getStatus() + ")");
        }

        // Responsible AI gate: flagged content must be fixed before review.
        String flag = aiAssistanceService.flagReason(problemAndSolution(project));
        if (flag != null) {
            throw new BadRequestException("AI moderation flagged this submission: " + flag
                    + ". Please edit the text and resubmit.");
        }
        if (project.isAiAssisted()
                && (project.getAiUsageNote() == null || project.getAiUsageNote().isBlank())) {
            throw new BadRequestException(
                    "AI assisted content must declare how AI was used (aiUsageNote) before submission");
        }

        project.submit();
        ImpactProject saved = projectRepository.save(project);
        notifyTeam(saved.getTeam(), "Impact project submitted",
                "\"" + saved.getTitle() + "\" was submitted for review.");
        return ProjectDtos.ProjectResponse.of(saved);
    }

    @Transactional
    public ProjectDtos.ProjectResponse review(Long id, String decision, String feedback) {
        User reviewer = requireStaff();
        ImpactProject project = find(id);

        String normalized = decision == null ? "" : decision.trim().toUpperCase();
        switch (normalized) {
            case "START" -> project.startReview();
            case "APPROVE" -> {
                if (project.getStatus() != ProjectStatus.UNDER_REVIEW
                        && project.getStatus() != ProjectStatus.SUBMITTED) {
                    throw new BadRequestException("Only submitted projects can be approved");
                }
                project.approve(feedback, reviewer);
            }
            case "REQUEST_CHANGES" -> {
                project.setStatus(ProjectStatus.DRAFT);
                project.setReviewFeedback(feedback);
            }
            default -> throw new BadRequestException("decision must be one of START, APPROVE, REQUEST_CHANGES");
        }
        return ProjectDtos.ProjectResponse.of(projectRepository.save(project));
    }

    /** Completion is the certification trigger - polymorphic via {@link Certifiable}. */
    @Transactional
    public ProjectDtos.ProjectResponse complete(Long id) {
        requireStaff();
        ImpactProject project = find(id);
        if (project.getStatus() != ProjectStatus.APPROVED) {
            throw new BadRequestException("Only an approved project can be completed");
        }
        project.complete();
        ImpactProject saved = projectRepository.save(project);

        List<CertificateDtos.CertificateResponse> issued = certificateService.issueFor(saved, saved.getTitle());
        notifyTeam(saved.getTeam(), "Impact project completed",
                "\"" + saved.getTitle() + "\" is complete. " + issued.size() + " certificate(s) issued.");
        return ProjectDtos.ProjectResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public ProjectDtos.ProjectResponse get(Long id) {
        return ProjectDtos.ProjectResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public ProjectDtos.ProjectResponse forTeam(Long teamId) {
        ImpactProject project = projectRepository.findByTeamId(teamId)
                .orElseThrow(() -> NotFoundException.resource("Project for team", teamId));
        return ProjectDtos.ProjectResponse.of(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectResponse> byStatus(ProjectStatus status) {
        return projectRepository.findByStatus(status).stream()
                .map(ProjectDtos.ProjectResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectDtos.ProjectResponse> mine() {
        Long userId = SecurityUtils.currentUserId();
        return teamRepository.findByParticipant(userId).stream()
                .map(Team::getImpactProject)
                .filter(Objects::nonNull)
                .map(ProjectDtos.ProjectResponse::of)
                .toList();
    }

    // ---------------------------------------------------------------------

    private void applyContent(ImpactProject project, ProjectDtos.ProjectRequest request) {
        project.setProblemStatement(request.problemStatement());
        project.setProposedSolution(request.proposedSolution());
        boolean aiAssisted = Boolean.TRUE.equals(request.aiAssisted());
        project.setAiAssisted(aiAssisted);
        project.setAiUsageNote(aiAssisted ? request.aiUsageNote() : null);
    }

    private String problemAndSolution(ImpactProject project) {
        return (project.getProblemStatement() == null ? "" : project.getProblemStatement()) + " "
                + (project.getProposedSolution() == null ? "" : project.getProposedSolution());
    }

    private void requireMemberOrStaff(Team team) {
        if (team == null) {
            throw new BadRequestException("The project has no team attached");
        }
        AuthenticatedUser principal = SecurityUtils.current();
        if (principal.isStaff()) {
            return;
        }
        if (!teamMemberRepository.existsByTeamIdAndParticipantId(team.getId(), principal.id())) {
            throw new ForbiddenException("Only members of the team can manage this project");
        }
    }

    private User requireStaff() {
        AuthenticatedUser principal = SecurityUtils.current();
        if (!principal.isStaff()) {
            throw new ForbiddenException("Only facilitators, mentors or administrators can perform this action");
        }
        return userRepository.findById(principal.id())
                .orElseThrow(() -> new ForbiddenException("Account not found"));
    }

    private ImpactProject find(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> NotFoundException.resource("Project", id));
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId).orElseThrow(() -> NotFoundException.resource("Team", teamId));
    }

    private void notifyTeam(Team team, String subject, String body) {
        if (team != null) {
            notificationDispatcher.dispatch(team, subject, body);
        }
    }
}
