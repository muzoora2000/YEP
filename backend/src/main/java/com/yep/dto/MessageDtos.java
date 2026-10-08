package com.yep.dto;

import java.time.OffsetDateTime;

import com.yep.domain.enums.AnnouncementScope;
import com.yep.domain.enums.AiModerationStatus;
import com.yep.domain.message.Announcement;
import com.yep.domain.message.Message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contracts for {@code /api/v1/messages}. */
public final class MessageDtos {

    private MessageDtos() {
    }

    public record MessageRequest(
            Long recipientId,
            Long teamId,
            Long exchangeId,
            @NotBlank(message = "body is required")
            @Size(max = 4000)
            String body,
            @Size(max = 200)
            String subject,
            Boolean aiGenerated) {
    }

    public record AnnouncementRequest(
            @NotBlank
            @Size(max = 200)
            String subject,

            @NotBlank
            @Size(max = 4000)
            String body,

            AnnouncementScope scope,
            Long exchangeId,
            Long teamId,
            Boolean aiGenerated) {
    }

    public record MessageResponse(
            Long id,
            String messageType,
            Long senderId,
            String senderName,
            Long recipientId,
            String recipientName,
            Long teamId,
            Long exchangeId,
            String subject,
            String body,
            boolean read,
            OffsetDateTime sentAt,
            boolean aiGenerated,
            String aiFlagReason,
            AiModerationStatus aiModerationStatus,
            AnnouncementScope scope,
            Boolean published,
            Boolean requiresHumanApproval) {

        public static MessageResponse of(Message message) {
            AnnouncementScope scope = null;
            Boolean published = null;
            Boolean requiresHumanApproval = null;
            if (message instanceof Announcement announcement) {
                scope = announcement.getScope();
                published = announcement.isPublished();
                requiresHumanApproval = announcement.isRequiresHumanApproval();
            }
            return new MessageResponse(
                    message.getId(),
                    message.getMessageType().name(),
                    message.getSender() != null ? message.getSender().getId() : null,
                    message.getSender() != null ? message.getSender().getFullName() : null,
                    message.getRecipient() != null ? message.getRecipient().getId() : null,
                    message.getRecipient() != null ? message.getRecipient().getFullName() : null,
                    message.getTeam() != null ? message.getTeam().getId() : null,
                    message.getExchange() != null ? message.getExchange().getId() : null,
                    message.getSubject(),
                    message.getBody(),
                    message.isRead(),
                    message.getSentAt(),
                    message.isAiGenerated(),
                    message.getAiFlagReason(),
                    message.getAiModerationStatus(),
                    scope,
                    published,
                    requiresHumanApproval);
        }
    }

    public record UnreadCountResponse(long unread, long pendingApprovals) {
    }
}
