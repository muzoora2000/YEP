package com.yep.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.Role;
import com.yep.domain.learning.DiscussionPost;
import com.yep.domain.learning.DiscussionThread;
import com.yep.domain.project.Team;
import com.yep.domain.user.User;
import com.yep.dto.LearningDtos.FlagRequest;
import com.yep.dto.LearningDtos.HidePostRequest;
import com.yep.dto.LearningDtos.ModerationQueueResponse;
import com.yep.dto.LearningDtos.ModerationRequest;
import com.yep.dto.LearningDtos.PostRequest;
import com.yep.dto.LearningDtos.PostResponse;
import com.yep.dto.LearningDtos.ThreadRequest;
import com.yep.dto.LearningDtos.ThreadResponse;
import com.yep.exception.BadRequestException;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.DiscussionPostRepository;
import com.yep.repository.DiscussionThreadRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.security.SecurityUtils;

@Service
public class DiscussionService {

    private static final Duration EDIT_WINDOW = Duration.ofMinutes(15);

    private static final List<String> DISALLOWED_WORDS = List.of("fuck", "shit", "bitch", "asshole", "bastard",
            "cunt", "dick", "whore", "slut", "wanker", "prick", "douche");

    private static final List<Pattern> DISALLOWED_PATTERNS = DISALLOWED_WORDS.stream()
            .map(word -> Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE))
            .toList();

    private final DiscussionThreadRepository threadRepository;
    private final DiscussionPostRepository postRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public DiscussionService(DiscussionThreadRepository threadRepository, DiscussionPostRepository postRepository,
            TeamRepository teamRepository, TeamMemberRepository teamMemberRepository,
            EnrollmentRepository enrollmentRepository, UserRepository userRepository, ActivityService activityService,
            SecurityUtils securityUtils, AuditService auditService, NotificationService notificationService) {
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ThreadResponse createThread(Long activityId, ThreadRequest request) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        DiscussionThread thread;
        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> new NotFoundException("TEAM_NOT_FOUND", "Team not found."));
            if (!team.getActivity().getActivityId().equals(activityId)) {
                throw new BadRequestException("TEAM_MISMATCH", "Team does not belong to this activity.");
            }
            thread = new DiscussionThread(activity, team, request.topic().trim());
        } else {
            thread = new DiscussionThread(activity, request.topic().trim());
        }
        thread = threadRepository.save(thread);
        auditService.record(currentUser(), "DISCUSSION_CREATED", "DiscussionThread", thread.getThreadId(),
                thread.getTopic());
        return toThreadResponse(thread);
    }

    @Transactional(readOnly = true)
    public List<ThreadResponse> listThreads(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        User actor = currentUser();
        boolean manager = isManager(activity, actor);
        if (!manager) {
            activityService.requireEnrolled(activity, actor.getUserId());
        }
        return threadRepository.findByActivityActivityId(activityId).stream()
                .filter(thread -> manager || canSeeTeamThread(thread, actor))
                .map(this::toThreadResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostResponse> posts(Long threadId) {
        DiscussionThread thread = findThread(threadId);
        User actor = currentUser();
        boolean manager = isManager(thread.getActivity(), actor);
        if (!manager) {
            activityService.requireEnrolled(thread.getActivity(), actor.getUserId());
            if (thread.getTeam() != null && !teamMemberRepository.existsByTeamTeamIdAndYouthUserId(
                    thread.getTeam().getTeamId(), actor.getUserId())) {
                throw new ForbiddenException("TEAM_ONLY", "This discussion is for team members only.");
            }
        }
        return postRepository.findByThreadThreadIdOrderByPostedAtAsc(threadId).stream()
                .filter(post -> manager || !post.isHidden())
                .map(this::toPostResponse)
                .toList();
    }

    @Transactional
    public PostResponse addPost(Long threadId, PostRequest request) {
        DiscussionThread thread = findThread(threadId);
        User actor = currentUser();
        boolean manager = isManager(thread.getActivity(), actor);
        if (!manager) {
            activityService.requireEnrolled(thread.getActivity(), actor.getUserId());
            if (thread.getTeam() != null && !teamMemberRepository.existsByTeamTeamIdAndYouthUserId(
                    thread.getTeam().getTeamId(), actor.getUserId())) {
                throw new ForbiddenException("TEAM_ONLY", "This discussion is for team members only.");
            }
        }
        DiscussionPost post = new DiscussionPost(thread, actor, sanitize(request.content()));
        post = postRepository.save(post);
        auditService.record(actor, "DISCUSSION_POSTED", "DiscussionThread", threadId, "Post " + post.getPostId());
        return toPostResponse(post);
    }

    @Transactional
    public PostResponse editPost(Long postId, PostRequest request) {
        DiscussionPost post = findPost(postId);
        User actor = currentUser();
        boolean manager = isManager(post.getThread().getActivity(), actor);
        if (!manager) {
            if (!post.getAuthor().getUserId().equals(actor.getUserId())) {
                throw new ForbiddenException("NOT_AUTHOR", "You can only edit your own posts.");
            }
            if (post.getPostedAt().plus(EDIT_WINDOW).isBefore(OffsetDateTime.now())) {
                throw new BusinessRuleException("EDIT_WINDOW_CLOSED",
                        "Posts can only be edited within 15 minutes of posting.");
            }
        }
        post.editPost(sanitize(request.content()));
        postRepository.save(post);
        auditService.record(actor, "DISCUSSION_EDITED", "DiscussionPost", postId, "Post edited");
        return toPostResponse(post);
    }

    @Transactional
    public PostResponse flagPost(Long postId, FlagRequest request) {
        DiscussionPost post = findPost(postId);
        User actor = currentUser();
        boolean manager = isManager(post.getThread().getActivity(), actor);
        if (!manager) {
            activityService.requireEnrolled(post.getThread().getActivity(), actor.getUserId());
        }
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "Flagged for review"
                : request.reason().trim();
        post.flag(reason);
        postRepository.save(post);
        auditService.record(actor, "DISCUSSION_FLAGGED", "DiscussionPost", postId, reason);
        userRepository.findByRole(Role.ADMIN).forEach(admin -> notificationService.notify(admin,
                "A discussion post was flagged in \"" + post.getThread().getTopic() + "\".",
                "/admin/moderation", NotificationType.IN_APP));
        return toPostResponse(post);
    }

    @Transactional(readOnly = true)
    public List<ModerationQueueResponse> moderationQueue() {
        User actor = currentUser();
        if (actor.getRole() != Role.ADMIN) {
            throw new ForbiddenException("ADMIN_ONLY", "Only administrators can view the moderation queue.");
        }
        return postRepository.findByFlaggedTrueAndHiddenFalseOrderByPostedAtAsc().stream()
                .map(this::toModerationRow)
                .toList();
    }

    @Transactional
    public PostResponse hidePost(Long postId, HidePostRequest request) {
        DiscussionPost post = findPost(postId);
        activityService.requireManage(post.getThread().getActivity());
        if (Boolean.TRUE.equals(request.hidden())) {
            post.hide();
        } else {
            post.unhide();
        }
        postRepository.save(post);
        auditService.record(currentUser(), "DISCUSSION_MODERATED", "DiscussionPost", postId,
                Boolean.TRUE.equals(request.hidden()) ? "Post hidden" : "Post visible");
        return toPostResponse(post);
    }

    @Transactional
    public PostResponse moderate(Long postId, ModerationRequest request) {
        DiscussionPost post = postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Discussion post not found."));
        activityService.requireManage(post.getThread().getActivity());
        if (request.flagReason() != null && !request.flagReason().isBlank()) {
            post.flag(request.flagReason().trim());
        }
        if (request.hidden()) {
            post.hide();
        } else {
            post.unhide();
        }
        postRepository.save(post);
        auditService.record(currentUser(), "DISCUSSION_MODERATED", "DiscussionPost", postId,
                request.hidden() ? "Post hidden" : "Post visible");
        return toPostResponse(post);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> flagged(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        return postRepository.findByFlaggedTrueAndHiddenFalseOrderByPostedAtAsc().stream()
                .filter(post -> post.getThread().getActivity().getActivityId().equals(activityId))
                .map(this::toPostResponse)
                .toList();
    }

    private DiscussionThread findThread(Long threadId) {
        return threadRepository.findById(threadId)
                .orElseThrow(() -> new NotFoundException("THREAD_NOT_FOUND", "Discussion thread not found."));
    }

    private DiscussionPost findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new NotFoundException("POST_NOT_FOUND", "Discussion post not found."));
    }

    private String sanitize(String content) {
        String trimmed = content.trim();
        boolean blocked = DISALLOWED_PATTERNS.stream().anyMatch(pattern -> pattern.matcher(trimmed).find());
        if (blocked) {
            throw new BusinessRuleException("INAPPROPRIATE_LANGUAGE",
                    "Your post contains language that is not allowed. Please revise it.");
        }
        return trimmed;
    }

    private ModerationQueueResponse toModerationRow(DiscussionPost post) {
        return new ModerationQueueResponse(post.getPostId(), post.getThread().getTopic(),
                post.getThread().getActivity().getTitle(), post.getAuthor().getName(), post.getContent(),
                post.getFlagReason(), post.getPostedAt(), post.isHidden());
    }

    private boolean isManager(LearningActivity activity, User actor) {
        return actor.getRole() == Role.ADMIN
                || (actor.getRole() == Role.FACILITATOR && activity.getFacilitator().getUserId().equals(actor.getUserId()));
    }

    private boolean canSeeTeamThread(DiscussionThread thread, User actor) {
        return thread.getTeam() == null
                || teamMemberRepository.existsByTeamTeamIdAndYouthUserId(thread.getTeam().getTeamId(), actor.getUserId());
    }

    private ThreadResponse toThreadResponse(DiscussionThread thread) {
        return new ThreadResponse(thread.getThreadId(), thread.getActivity().getActivityId(),
                thread.getTeam() == null ? null : thread.getTeam().getTeamId(), thread.getTopic(),
                thread.getCreatedAt(), thread.getPosts().size());
    }

    private PostResponse toPostResponse(DiscussionPost post) {
        return new PostResponse(post.getPostId(), post.getThread().getThreadId(), post.getAuthor().getUserId(),
                post.getAuthor().getName(), post.getContent(), post.getPostedAt(), post.isFlagged(),
                post.getFlagReason(), post.isHidden());
    }

    private User currentUser() {
        return userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
    }
}
