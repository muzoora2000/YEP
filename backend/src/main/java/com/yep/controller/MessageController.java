package com.yep.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.dto.MessageDtos;
import com.yep.dto.PageResponse;
import com.yep.service.MessageService;

import jakarta.validation.Valid;

/** Messaging and announcements - {@code /api/v1/messages}. */
@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public PageResponse<MessageDtos.MessageResponse> inbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return messageService.inbox(page, size);
    }

    @PostMapping
    public ResponseEntity<MessageDtos.MessageResponse> send(
            @Valid @RequestBody MessageDtos.MessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.send(request));
    }

    @PostMapping("/{id}/read")
    public MessageDtos.MessageResponse markRead(@PathVariable Long id) {
        return messageService.markRead(id);
    }

    @GetMapping("/announcements")
    public PageResponse<MessageDtos.MessageResponse> announcements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return messageService.announcements(page, size);
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public ResponseEntity<MessageDtos.MessageResponse> announce(
            @Valid @RequestBody MessageDtos.AnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.announce(request));
    }

    /** Human approval for AI drafted announcements (responsible-AI workflow). */
    @PostMapping("/announcements/{id}/approve")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public MessageDtos.MessageResponse approve(@PathVariable Long id) {
        return messageService.approveAnnouncement(id);
    }

    @GetMapping("/pending-approval")
    @PreAuthorize("hasAnyRole('FACILITATOR', 'MENTOR', 'ADMIN')")
    public List<MessageDtos.MessageResponse> pendingApproval() {
        return messageService.pendingApproval();
    }

    @GetMapping("/unread-count")
    public MessageDtos.UnreadCountResponse unreadCount() {
        return messageService.unreadCounts();
    }
}
