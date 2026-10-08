package com.yep.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AiModerationStatus;
import com.yep.domain.enums.AnnouncementScope;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.message.Announcement;
import com.yep.domain.message.DirectMessage;
import com.yep.domain.message.Message;
import com.yep.domain.message.TeamMessage;
import com.yep.domain.team.Team;
import com.yep.domain.user.User;
import com.yep.dto.MessageDtos;
import com.yep.dto.PageResponse;
import com.yep.exception.BadRequestException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.MessageRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.repository.VirtualExchangeRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

/**
 * Messaging, announcements and the responsible-AI approval workflow.
 *
 * <p>AI drafted announcements are stored <b>unpublished</b> and only become
 * visible after a human ({@link #approveAnnouncement}) flips the approval flag.</p>
 */
@Service
public class MessageService {

    private static final int MAX_PAGE_SIZE = 50;

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final VirtualExchangeRepository exchangeRepository;
    private final AiAssistanceService aiAssistanceService;
    private final NotificationDispatcher notificationDispatcher;

    public MessageService(
            MessageRepository messageRepository,
            UserRepository userRepository,
            TeamRepository teamRepository,
            VirtualExchangeRepository exchangeRepository,
            AiAssistanceService aiAssistanceService,
            NotificationDispatcher notificationDispatcher) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.exchangeRepository = exchangeRepository;
        this.aiAssistanceService = aiAssistanceService;
        this.notificationDispatcher = notificationDispatcher;
    }

    // ------------------------------------------------------------------
    // Direct / team messages
    // ------------------------------------------------------------------

    @Transactional
    public MessageDtos.MessageResponse send(MessageDtos.MessageRequest request) {
        AuthenticatedUser principal = SecurityUtils.current();
        User sender = userRepository.findById(principal.id())
                .orElseThrow(() -> new ForbiddenException("Account not found"));

        boolean aiGenerated = Boolean.TRUE.equals(request.aiGenerated());
        Message message;

        if (request.teamId() != null) {
            Team team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> NotFoundException.resource("Team", request.teamId()));
            if (!principal.isAdmin() && !principal.isStaff()
                    && !teamMemberPresent(team, principal.id())) {
                throw new ForbiddenException("You can only message teams you belong to");
            }
            TeamMessage teamMessage = TeamMessage.toTeam(sender, team, request.subject(), request.body());
            teamMessage.setAiGenerated(aiGenerated);
            message = teamMessage;
        } else if (request.recipientId() != null) {
            User recipient = userRepository.findById(request.recipientId())
                    .orElseThrow(() -> NotFoundException.resource("User", request.recipientId()));
            DirectMessage direct = DirectMessage.between(sender, recipient, request.subject(), request.body());
            direct.setAiGenerated(aiGenerated);
            message = direct;
        } else {
            throw new BadRequestException("Provide either recipientId or teamId");
        }

        applyModeration(message);
        return MessageDtos.MessageResponse.of(messageRepository.save(message));
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageDtos.MessageResponse> inbox(int page, int size) {
        Long userId = SecurityUtils.currentUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<Message> result = messageRepository.findInbox(userId, pageable);
        return PageResponse.from(result.map(MessageDtos.MessageResponse::of));
    }

    @Transactional
    public MessageDtos.MessageResponse markRead(Long id) {
        Long userId = SecurityUtils.currentUserId();
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> NotFoundException.resource("Message", id));
        boolean addressedToMe = (message.getRecipient() != null && message.getRecipient().getId().equals(userId))
                || (message.getTeam() != null && teamMemberPresent(message.getTeam(), userId));
        if (!addressedToMe && !SecurityUtils.current().isAdmin()) {
            throw new ForbiddenException("This message is not addressed to you");
        }
        message.markRead();
        return MessageDtos.MessageResponse.of(messageRepository.save(message));
    }

    // ------------------------------------------------------------------
    // Announcements
    // ------------------------------------------------------------------

    /**
     * Creates an announcement. When {@code aiGenerated} is true the announcement
     * is queued for human approval instead of being published.
     */
    @Transactional
    public MessageDtos.MessageResponse announce(MessageDtos.AnnouncementRequest request) {
        AuthenticatedUser principal = SecurityUtils.current();
        if (!principal.isStaff()) {
            throw new ForbiddenException("Only facilitators, mentors or administrators can announce");
        }
        User sender = userRepository.findById(principal.id())
                .orElseThrow(() -> new ForbiddenException("Account not found"));

        boolean aiGenerated = Boolean.TRUE.equals(request.aiGenerated());
        AnnouncementScope scope = request.scope() == null ? AnnouncementScope.ALL_USERS : request.scope();

        Announcement announcement = switch (scope) {
            case EXCHANGE_COHORT -> {
                VirtualExchange exchange = request.exchangeId() == null ? null
                        : exchangeRepository.findById(request.exchangeId())
                                .orElseThrow(() -> NotFoundException.resource("Exchange", request.exchangeId()));
                if (exchange == null) {
                    throw new BadRequestException("exchangeId is required for an EXCHANGE_COHORT announcement");
                }
                yield Announcement.forExchange(sender, exchange, request.subject(), request.body(), aiGenerated);
            }
            case TEAM -> {
                Team team = request.teamId() == null ? null
                        : teamRepository.findById(request.teamId())
                                .orElseThrow(() -> NotFoundException.resource("Team", request.teamId()));
                if (team == null) {
                    throw new BadRequestException("teamId is required for a TEAM announcement");
                }
                yield Announcement.forTeam(sender, team, request.subject(), request.body(), aiGenerated);
            }
            case ALL_USERS -> Announcement.byUser(sender, scope, request.subject(), request.body(), aiGenerated);
        };

        applyModeration(announcement);
        Announcement saved = messageRepository.save(announcement);
        notifyTarget(saved);
        return MessageDtos.MessageResponse.of(saved);
    }

    /** Human approval step for AI drafted announcements. */
    @Transactional
    public MessageDtos.MessageResponse approveAnnouncement(Long id) {
        AuthenticatedUser principal = SecurityUtils.current();
        if (!principal.isStaff()) {
            throw new ForbiddenException("Only facilitators, mentors or administrators can approve announcements");
        }
        Message message = messageRepository.findById(id)
                .orElseThrow(() -> NotFoundException.resource("Message", id));
        if (!(message instanceof Announcement announcement)) {
            throw new BadRequestException("Only announcements require approval");
        }
        User approver = userRepository.findById(principal.id())
                .orElseThrow(() -> new ForbiddenException("Account not found"));
        announcement.approveBy(approver);
        Announcement saved = messageRepository.save(announcement);
        notifyTarget(saved);
        return MessageDtos.MessageResponse.of(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageDtos.MessageResponse> announcements(int page, int size) {
        Long userId = SecurityUtils.currentUserId();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));

        List<Message> merged = new ArrayList<>();
        messageRepository.findGlobalAnnouncements(pageable).forEach(merged::add);
        messageRepository.findCohortAnnouncements(userId, pageable).forEach(merged::add);

        List<MessageDtos.MessageResponse> projected = merged.stream()
                .map(MessageDtos.MessageResponse::of)
                .distinct()
                .sorted(java.util.Comparator.comparing(MessageDtos.MessageResponse::sentAt,
                        java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
        return PageResponse.of(projected);
    }

    @Transactional(readOnly = true)
    public List<MessageDtos.MessageResponse> pendingApproval() {
        requireStaff();
        return messageRepository.findPendingApproval().stream()
                .map(MessageDtos.MessageResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public MessageDtos.UnreadCountResponse unreadCounts() {
        Long userId = SecurityUtils.currentUserId();
        long unread = messageRepository.countByRecipientIdAndReadFalse(userId);
        long pending = SecurityUtils.current().isStaff() ? messageRepository.findPendingApproval().size() : 0;
        return new MessageDtos.UnreadCountResponse(unread, pending);
    }

    // ---------------------------------------------------------------------

    private void applyModeration(Message message) {
        String reason = aiAssistanceService.flagReason(message.getBody());
        if (reason != null) {
            message.setAiFlagReason(reason);
            message.setAiModerationStatus(AiModerationStatus.FLAGGED);
            throw new BadRequestException("Automated AI moderation flagged this content: " + reason
                    + ". A human reviewer must approve it before it can be sent.");
        }
        if (message.isAiGenerated()) {
            message.setAiModerationStatus(AiModerationStatus.NOT_SCREENED);
        } else {
            message.setAiModerationStatus(AiModerationStatus.PASSED);
        }
    }

    private void notifyTarget(Announcement announcement) {
        if (!announcement.isVisible()) {
            return;
        }
        switch (announcement.getScope()) {
            case TEAM -> notificationDispatcher.dispatch(announcement.getTeam(),
                    announcement.getSubject(), announcement.getBody());
            default -> { /* global / cohort announcements are read from the list endpoint */ }
        }
    }

    private boolean teamMemberPresent(Team team, Long participantId) {
        return team.getTeamMembers() != null && team.getTeamMembers().stream()
                .anyMatch(tm -> tm.getParticipant() != null && tm.getParticipant().getId().equals(participantId));
    }

    private void requireStaff() {
        if (!SecurityUtils.current().isStaff()) {
            throw new ForbiddenException("Staff role required");
        }
    }
}
