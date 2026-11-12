package com.yep.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yep.domain.enums.SessionStatus;
import com.yep.dto.LearningDtos.AttendanceBulkRequest;
import com.yep.dto.LearningDtos.AttendanceResponse;
import com.yep.dto.LearningDtos.SessionRequest;
import com.yep.dto.LearningDtos.SessionResponse;
import com.yep.service.SessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/activities/{activityId}/sessions")
    public List<SessionResponse> list(@PathVariable Long activityId) {
        return sessionService.list(activityId);
    }

    @PostMapping("/activities/{activityId}/sessions")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<SessionResponse> create(@PathVariable Long activityId,
            @Valid @RequestBody SessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sessionService.create(activityId, request));
    }

    @PutMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public SessionResponse update(@PathVariable Long sessionId, @Valid @RequestBody SessionRequest request) {
        return sessionService.update(sessionId, request);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long sessionId) {
        sessionService.delete(sessionId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/sessions/{sessionId}/status")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public SessionResponse changeStatus(@PathVariable Long sessionId, @RequestBody SessionStatusRequest request) {
        return sessionService.changeStatus(sessionId, request.status());
    }

    @PostMapping("/sessions/{sessionId}/check-in")
    public AttendanceResponse checkIn(@PathVariable Long sessionId) {
        return sessionService.checkIn(sessionId);
    }

    @GetMapping("/sessions/{sessionId}/attendance")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public List<AttendanceResponse> attendance(@PathVariable Long sessionId) {
        return sessionService.attendance(sessionId);
    }

    @PutMapping("/sessions/{sessionId}/attendance")
    @PreAuthorize("hasAnyRole('FACILITATOR','ADMIN')")
    public List<AttendanceResponse> markAttendance(@PathVariable Long sessionId,
            @Valid @RequestBody AttendanceBulkRequest request) {
        return sessionService.markAttendance(sessionId, request);
    }

    @GetMapping("/attendance/me")
    public List<AttendanceResponse> myAttendance() {
        return sessionService.myAttendance();
    }

    public record SessionStatusRequest(SessionStatus status) {
    }
}
