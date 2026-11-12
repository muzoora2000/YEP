package com.yep.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.PartnerDtos.NotificationResponse;
import com.yep.dto.PartnerDtos.UnreadCountResponse;
import com.yep.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationResponse> list() {
        return notificationService.myNotifications();
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(notificationService.unreadCount());
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable Long notificationId) {
        return notificationService.markRead(notificationId);
    }

    @PutMapping("/read-all")
    public void markAllRead() {
        notificationService.markAllRead();
    }
}
