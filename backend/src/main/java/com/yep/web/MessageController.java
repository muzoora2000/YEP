package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.domain.enums.NotificationType;
import com.yep.dto.PartnerDtos.NotificationResponse;
import com.yep.dto.PartnerDtos.SendMessageRequest;
import com.yep.dto.PartnerDtos.UnreadCountResponse;
import com.yep.service.NotificationService;

import jakarta.validation.Valid;

/** Spec-compatible messaging surface, backed by the notification subsystem. */
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final NotificationService notificationService;

    public MessageController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> inbox() {
        return notificationService.myNotifications();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(notificationService.unreadCount());
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.notifyUser(
                request.recipientId(), request.message(), request.link(), NotificationType.IN_APP));
    }

    @PutMapping("/{messageId}/read")
    public NotificationResponse markRead(@PathVariable Long messageId) {
        return notificationService.markRead(messageId);
    }

    @PutMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }
}
