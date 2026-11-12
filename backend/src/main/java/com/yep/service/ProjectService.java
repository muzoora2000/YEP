package com.yep.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.ReviewDecision;
import com.yep.domain.enums.Role;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.project.Milestone;
import com.yep.domain.project.ProjectReview;
import com.yep.domain.project.Team;
import com.yep.domain.user.User;
import com.yep.dto.ProjectDtos.MilestoneRequest;
import com.yep.dto.ProjectDtos.MilestoneResponse;
import com.yep.dto.ProjectDtos.MilestoneStatusRequest;
import com.yep.dto.ProjectDtos.ProjectRequest;
import com.yep.dto.ProjectDtos.ProjectResponse;
import com.yep.dto.ProjectDtos.ProjectUpdateRequest;
import com.yep.dto.ProjectDtos.ReviewRequest;
import com.yep.dto.ProjectDtos.ReviewResponse;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.MilestoneRepository;
import com.yep.repository.ProjectReviewRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

@Service
public class ProjectService {

    private final ImpactProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final ProjectReviewRepository reviewRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamService teamService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ProjectService(ImpactProjectRepository projectRepository, MilestoneRepository milestoneRepository,
            ProjectReviewRepository reviewRepository, TeamMemberRepository teamMemberRepository,
            TeamService teamService, UserRepository userRepository, SecurityUtils securityUtils,
            AuditService auditService, NotificationService notificationService) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.reviewRepository = reviewRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamService = teamService;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest request) {
        Team team = teamService.find(request.teamId());
        requireMemberOrManager(team);
        if (projectRepository.findByTeamTeamId(team.getTeamId()).isPresent()) {
            throw new ConflictException("PROJECT_EXISTS", "This team already has an impact project.");
        }
        ImpactProject project = new ImpactProject(team, request.title().trim());
        project.createProject(trimToNull(request.description()), request.challengeArea());
        project.setAiUsed(Boolean.TRUE.equals(request.aiUsed()));
        project.setAiUsageDeclaration(trimToNull(request.aiUsageDeclaration()));
        validateAiDeclaration(project);
        project = projectRepository.save(project);
        auditService.record(currentActor(), "PROJECT_CREATED", "ImpactProject", project.getProjectId(),
                project.getTitle());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(Long projectId) {
        ImpactProject project = find(projectId);
        teamService.requireView(project.getTeam());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getByTeam(Long teamId) {
        Team team = teamService.find(teamId);
        teamService.requireView(team);
        ImpactProject project = projectRepository.findByTeamTeamId(teamId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "This team has no project yet."));
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> myProjects() {
        Long userId = securityUtils.currentUserId();
        return teamMemberRepository.findByYouthUserId(userId).stream()
                .map(member -> projectRepository.findByTeamTeamId(member.getTeam().getTeamId()).orElse(null))
                .filter(project -> project != null)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> mentorProjects() {
        AuthenticatedUser actor = securityUtils.currentUser();
        if (actor.role() != Role.MENTOR) {
            throw new ForbiddenException("MENTOR_ONLY", "Mentor account required.");
        }
        return projectRepository.findByTeamMentorUserId(actor.userId()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> showcase() {
        return projectRepository.findByOpenForSupportTrueAndStatusIn(
                List.of(ProjectStatus.APPROVED, ProjectStatus.COMPLETED)).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ProjectResponse update(Long projectId, ProjectUpdateRequest request) {
        ImpactProject project = find(projectId);
        requireMemberOrManager(project.getTeam());
        if (!project.isEditable()) {
            throw new BusinessRuleException("PROJECT_NOT_EDITABLE",
                    "Project can only be edited while DRAFT or CHANGES_REQUESTED.");
        }
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BusinessRuleException("INVALID_TITLE", "Project title cannot be blank.");
            }
            project.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            project.setDescription(trimToNull(request.description()));
        }
        if (request.challengeArea() != null) {
            project.setChallengeArea(request.challengeArea());
        }
        if (request.aiUsed() != null) {
            project.setAiUsed(request.aiUsed());
        }
        if (request.aiUsageDeclaration() != null) {
            project.setAiUsageDeclaration(trimToNull(request.aiUsageDeclaration()));
        }
        validateAiDeclaration(project);
        projectRepository.save(project);
        auditService.record(currentActor(), "PROJECT_UPDATED", "ImpactProject", projectId, project.getTitle());
        return toResponse(project);
    }

    @Transactional
    public MilestoneResponse addMilestone(Long projectId, MilestoneRequest request) {
        ImpactProject project = find(projectId);
        requireMemberOrManager(project.getTeam());
        if (!project.isEditable()) {
            throw new BusinessRuleException("PROJECT_NOT_EDITABLE",
                    "Milestones can only be added while the project is editable.");
        }
        Milestone milestone = new Milestone(project, request.title().trim(), request.dueDate());
        milestone = milestoneRepository.save(milestone);
        project.getMilestones().add(milestone);
        auditService.record(currentActor(), "MILESTONE_CREATED", "Milestone", milestone.getMilestoneId(),
                milestone.getTitle());
        return toMilestoneResponse(milestone);
    }

    @Transactional
    public MilestoneResponse updateMilestone(Long milestoneId, MilestoneStatusRequest request) {
        Milestone milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new NotFoundException("MILESTONE_NOT_FOUND", "Milestone not found."));
        ImpactProject project = milestone.getProject();
        if (!isTeamMember(project.getTeam()) && !isManager(project.getTeam())) {
            teamService.requireView(project.getTeam());
            throw new ForbiddenException("FORBIDDEN", "Only the team, mentor or facilitator can update milestones.");
        }
        milestone.updateMilestone(request.status());
        milestoneRepository.save(milestone);
        auditService.record(currentActor(), "MILESTONE_UPDATED", "Milestone", milestoneId,
                milestone.getStatus().name());
        return toMilestoneResponse(milestone);
    }

    @Transactional
    public ProjectResponse submit(Long projectId) {
        ImpactProject project = find(projectId);
        requireMemberOrManager(project.getTeam());
        if (project.getTeam().memberCount() < project.getTeam().getActivity().getMinTeamSize()) {
            throw new BusinessRuleException("TEAM_TOO_SMALL", "The team must reach the minimum size before submitting.");
        }
        try {
            project.submitProject();
        } catch (IllegalStateException exception) {
            throw new BusinessRuleException("PROJECT_NOT_READY", exception.getMessage());
        }
        projectRepository.save(project);
        if (project.getTeam().getMentor() != null) {
            notificationService.notify(project.getTeam().getMentor(),
                    "Project submitted for review: " + project.getTitle(),
                    "/projects/" + projectId, NotificationType.IN_APP);
        }
        notifyTeam(project.getTeam(), "Your project " + project.getTitle() + " was submitted for review.",
                "/projects/" + projectId);
        auditService.record(currentActor(), "PROJECT_SUBMITTED", "ImpactProject", projectId, project.getTitle());
        return toResponse(project);
    }

    @Transactional
    public ReviewResponse review(Long projectId, ReviewRequest request) {
        ImpactProject project = find(projectId);
        Team team = project.getTeam();
        AuthenticatedUser actor = securityUtils.currentUser();
        if (actor.role() != Role.MENTOR || team.getMentor() == null
                || !team.getMentor().getUserId().equals(actor.userId())) {
            throw new ForbiddenException("MENTOR_ONLY", "Only the assigned mentor can review this project.");
        }
        if (project.getStatus() == ProjectStatus.SUBMITTED) {
            project.openForReview();
        }
        if (project.getStatus() != ProjectStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("PROJECT_NOT_UNDER_REVIEW", "Only a submitted project can be reviewed.");
        }
        if (request.decision() == ReviewDecision.CHANGES_REQUESTED) {
            project.requestChanges();
        } else if (request.decision() == ReviewDecision.APPROVED) {
            project.approve();
        }
        projectRepository.save(project);
        ProjectReview review = reviewRepository.save(new ProjectReview(project, team.getMentor(),
                request.decision(), request.feedback().trim()));
        notifyTeam(team, "Your project " + project.getTitle() + " was reviewed: "
                + request.decision().name().replace('_', ' ') + ".", "/projects/" + projectId);
        auditService.record(currentActor(), "PROJECT_REVIEWED", "ImpactProject", projectId,
                request.decision().name());
        return toReviewResponse(review);
    }

    @Transactional
    public ProjectResponse setSupportVisibility(Long projectId, boolean openForSupport) {
        ImpactProject project = find(projectId);
        requireMemberOrManager(project.getTeam());
        if (openForSupport && project.getStatus() != ProjectStatus.APPROVED
                && project.getStatus() != ProjectStatus.COMPLETED) {
            throw new BusinessRuleException("PROJECT_NOT_APPROVED",
                    "Only approved projects can be opened to partner support.");
        }
        project.setOpenForSupport(openForSupport);
        projectRepository.save(project);
        auditService.record(currentActor(), "PROJECT_SUPPORT_VISIBILITY_CHANGED", "ImpactProject", projectId,
                String.valueOf(openForSupport));
        return toResponse(project);
    }

    @Transactional
    public ProjectResponse complete(Long projectId) {
        ImpactProject project = find(projectId);
        teamService.requireManage(project.getTeam());
        if (project.getStatus() != ProjectStatus.APPROVED) {
            throw new BusinessRuleException("PROJECT_NOT_APPROVED", "Only an approved project can be completed.");
        }
        project.complete();
        projectRepository.save(project);
        notifyTeam(project.getTeam(), "Your project " + project.getTitle() + " has been marked complete.",
                "/projects/" + projectId);
        auditService.record(currentActor(), "PROJECT_COMPLETED", "ImpactProject", projectId, project.getTitle());
        return toResponse(project);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> reviews(Long projectId) {
        ImpactProject project = find(projectId);
        teamService.requireView(project.getTeam());
        return reviewRepository.findByProjectProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(this::toReviewResponse)
                .toList();
    }

    public ImpactProject find(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("PROJECT_NOT_FOUND", "Project not found."));
    }

    public ProjectResponse toResponse(ImpactProject project) {
        List<MilestoneResponse> milestones = milestoneRepository.findByProjectProjectId(project.getProjectId()).stream()
                .map(this::toMilestoneResponse)
                .toList();
        List<ReviewResponse> reviews = reviewRepository.findByProjectProjectIdOrderByCreatedAtAsc(
                project.getProjectId()).stream().map(this::toReviewResponse).toList();
        return new ProjectResponse(project.getProjectId(), project.getTeam().getTeamId(),
                project.getTeam().getTeamName(), project.getTeam().getActivity().getActivityId(),
                project.getTeam().getActivity().getTitle(), project.getTitle(), project.getDescription(),
                project.getChallengeArea(), project.getStatus(), project.isAiUsed(),
                project.getAiUsageDeclaration(), project.getSubmittedAt(), project.isOpenForSupport(),
                project.progressPercent(), project.hasOverdueMilestone(LocalDate.now()), milestones, reviews);
    }

    private MilestoneResponse toMilestoneResponse(Milestone milestone) {
        return new MilestoneResponse(milestone.getMilestoneId(), milestone.getProject().getProjectId(),
                milestone.getTitle(), milestone.getDueDate(), milestone.getStatus(), milestone.getCompletedAt(),
                milestone.isOverdue(LocalDate.now()));
    }

    private ReviewResponse toReviewResponse(ProjectReview review) {
        return new ReviewResponse(review.getReviewId(), review.getProject().getProjectId(),
                review.getMentor().getUserId(), review.getMentor().getName(), review.getDecision(),
                review.getFeedback(), review.getCreatedAt());
    }

    private void validateAiDeclaration(ImpactProject project) {
        if (project.isAiUsed() && (project.getAiUsageDeclaration() == null
                || project.getAiUsageDeclaration().isBlank())) {
            throw new BusinessRuleException("AI_DECLARATION_REQUIRED",
                    "An AI usage declaration is required when AI was used.");
        }
    }

    private void requireMemberOrManager(Team team) {
        if (!isTeamMember(team) && !isManager(team)) {
            throw new ForbiddenException("FORBIDDEN", "Only the team, mentor or facilitator can do that.");
        }
    }

    private boolean isTeamMember(Team team) {
        Long userId = securityUtils.currentUserId();
        return team.getMembers().stream().anyMatch(member -> member.getYouth().getUserId().equals(userId));
    }

    private boolean isManager(Team team) {
        AuthenticatedUser actor = securityUtils.currentUser();
        if (actor.role() == Role.ADMIN) {
            return true;
        }
        if (actor.role() == Role.FACILITATOR
                && team.getActivity().getFacilitator().getUserId().equals(actor.userId())) {
            return true;
        }
        return team.getMentor() != null && team.getMentor().getUserId().equals(actor.userId());
    }

    private void notifyTeam(Team team, String message, String link) {
        team.getMembers().forEach(member -> notificationService.notify(member.getYouth(), message, link,
                NotificationType.IN_APP));
    }

    private User currentActor() {
        return userRepository.findById(securityUtils.currentUserId()).orElse(null);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
