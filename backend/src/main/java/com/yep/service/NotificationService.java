package com.yep.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.NotificationType;
import com.yep.domain.notification.EmailNotification;
import com.yep.domain.notification.InAppNotification;
import com.yep.domain.notification.Notification;
import com.yep.domain.notification.SmsNotification;
import com.yep.domain.user.User;
import com.yep.dto.PartnerDtos.NotificationResponse;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.NotificationRepository;
import com.yep.repository.UserRepository;
import com.yep.security.SecurityUtils;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final EmailGateway emailGateway;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
            SecurityUtils securityUtils, EmailGateway emailGateway) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.emailGateway = emailGateway;
    }

    @Transactional
    public NotificationResponse notify(User recipient, String message, String link, NotificationType type) {
        if (recipient == null) {
            return null;
        }
        NotificationType resolved = type == null ? NotificationType.IN_APP : type;
        Notification notification = switch (resolved) {
            case EMAIL -> new EmailNotification(recipient, message, link);
            case SMS -> new SmsNotification(recipient, message, link);
            default -> new InAppNotification(recipient, message, link);
        };
        notification = notificationRepository.save(notification);
        if (resolved == NotificationType.EMAIL) {
            emailGateway.send(recipient.getEmail(), "YEP notification", message + (link == null ? "" : "\n" + link));
        } else if (resolved == NotificationType.SMS) {
            log.info("SMS notification queued for {}: {}", recipient.getPhone(), message);
        }
        return toResponse(notification);
    }

    @Transactional
    public NotificationResponse notifyUser(Long userId, String message, String link, NotificationType type) {
        User recipient = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
        return notify(recipient, message, link, type);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> myNotifications() {
        return notificationRepository.findByRecipientUserIdOrderByDateSentDesc(securityUtils.currentUserId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByRecipientUserIdAndReadFalse(securityUtils.currentUserId());
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("NOTIFICATION_NOT_FOUND", "Notification not found."));
        if (!notification.getRecipient().getUserId().equals(securityUtils.currentUserId())) {
            throw new ForbiddenException("FORBIDDEN", "You cannot access this notification.");
        }
        notification.markRead();
        notificationRepository.save(notification);
        return toResponse(notification);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.findByRecipientUserIdAndReadFalseOrderByDateSentDesc(securityUtils.currentUserId())
                .forEach(notification -> {
                    notification.markRead();
                    notificationRepository.save(notification);
                });
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getNotificationId(), notification.getType(),
                notification.getMessage(), notification.getLink(), notification.getDateSent(), notification.isRead());
    }
}
