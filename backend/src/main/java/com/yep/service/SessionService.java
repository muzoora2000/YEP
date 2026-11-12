package com.yep.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.AttendanceStatus;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.SessionStatus;
import com.yep.domain.learning.Attendance;
import com.yep.domain.learning.VirtualSession;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.AttendanceBulkRequest;
import com.yep.dto.LearningDtos.AttendanceEntry;
import com.yep.dto.LearningDtos.AttendanceResponse;
import com.yep.dto.LearningDtos.SessionRequest;
import com.yep.dto.LearningDtos.SessionResponse;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.AttendanceRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.repository.VirtualSessionRepository;
import com.yep.security.SecurityUtils;

@Service
public class SessionService {

    private final VirtualSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ActivityService activityService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public SessionService(VirtualSessionRepository sessionRepository, AttendanceRepository attendanceRepository,
            EnrollmentRepository enrollmentRepository, UserRepository userRepository, TeamRepository teamRepository,
            ActivityService activityService, SecurityUtils securityUtils, AuditService auditService,
            NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.activityService = activityService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public SessionResponse create(Long activityId, SessionRequest request) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        User actor = currentUser();
        VirtualSession session = new VirtualSession(activity, request.title().trim(), request.scheduledStart(),
                request.durationMinutes(), request.meetingLink().trim(), actor);
        session = sessionRepository.save(session);
        auditService.record(actor, "SESSION_CREATED", "VirtualSession", session.getSessionId(), session.getTitle());
        notifyEnrolled(activity,
                "New session scheduled for " + activity.getTitle() + ": " + session.getTitle() + " at "
                        + session.getScheduledStart() + ".");
        return toResponse(session);
    }

    @Transactional
    public SessionResponse update(Long sessionId, SessionRequest request) {
        VirtualSession session = find(sessionId);
        activityService.requireManage(session.getActivity());
        OffsetDateTime previousStart = session.getScheduledStart();
        String previousLink = session.getMeetingLink();
        session.setTitle(request.title().trim());
        session.setScheduledStart(request.scheduledStart());
        session.setDurationMinutes(request.durationMinutes());
        session.setMeetingLink(request.meetingLink().trim());
        sessionRepository.save(session);
        auditService.record(currentUser(), "SESSION_UPDATED", "VirtualSession", sessionId, session.getTitle());
        boolean timeOrLinkChanged = previousStart == null ? request.scheduledStart() != null
                : !previousStart.equals(request.scheduledStart())
                        || (previousLink == null ? request.meetingLink() != null
                                : !previousLink.equals(request.meetingLink().trim()));
        if (timeOrLinkChanged) {
            notifyEnrolled(session.getActivity(),
                    "Session update for " + session.getActivity().getTitle() + ": " + session.getTitle()
                            + " is now scheduled at " + session.getScheduledStart() + ".");
        }
        return toResponse(session);
    }

    @Transactional
    public void delete(Long sessionId) {
        VirtualSession session = find(sessionId);
        activityService.requireManage(session.getActivity());
        if (attendanceRepository.countBySessionSessionId(sessionId) > 0) {
            throw new BusinessRuleException("SESSION_HAS_ATTENDANCE",
                    "Sessions with recorded attendance cannot be deleted.");
        }
        sessionRepository.delete(session);
        auditService.record(currentUser(), "SESSION_DELETED", "VirtualSession", sessionId, session.getTitle());
    }

    @Transactional
    public SessionResponse changeStatus(Long sessionId, SessionStatus status) {
        VirtualSession session = find(sessionId);
        activityService.requireManage(session.getActivity());
        session.setStatus(status);
        sessionRepository.save(session);
        auditService.record(currentUser(), "SESSION_STATUS_CHANGED", "VirtualSession", sessionId,
                "Status set to " + status);
        return toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> list(Long activityId) {
        LearningActivity activity = activityService.find(activityId);
        User actor = currentUser();
        boolean manager = actor.getRole() == Role.ADMIN || (actor.getRole() == Role.FACILITATOR
                && activity.getFacilitator().getUserId().equals(actor.getUserId()));
        boolean mentor = actor.getRole() == Role.MENTOR
                && teamRepository.existsByActivityActivityIdAndMentorUserId(activityId, actor.getUserId());
        if (manager) {
            return sessionRepository.findByActivityActivityIdOrderByScheduledStartAsc(activityId).stream()
                    .map(this::toResponse).toList();
        }
        if (!mentor) {
            activityService.requireEnrolled(activity, actor.getUserId());
        }
        return sessionRepository.findByActivityActivityIdOrderByScheduledStartAsc(activityId).stream()
                .filter(session -> session.getStatus() != SessionStatus.CANCELLED)
                .map(this::toResponse).toList();
    }

    @Transactional
    public AttendanceResponse checkIn(Long sessionId) {
        VirtualSession session = find(sessionId);
        YouthParticipant youth = currentParticipant();
        activityService.requireEnrolled(session.getActivity(), youth.getUserId());
        if (session.getStatus() == SessionStatus.CANCELLED || session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessRuleException("CHECK_IN_CLOSED", "Check-in is not available for this session.");
        }
        if (!session.isCheckInOpen(OffsetDateTime.now())) {
            throw new BusinessRuleException("CHECK_IN_CLOSED", "Check-in opens 10 minutes before the session.");
        }
        Attendance attendance = attendanceRepository.findBySessionSessionIdAndYouthUserId(sessionId, youth.getUserId())
                .orElseGet(() -> new Attendance(session, youth, AttendanceStatus.PRESENT));
        attendance.mark(AttendanceStatus.PRESENT);
        attendance = attendanceRepository.save(attendance);
        auditService.record(youth, "ATTENDANCE_CHECKED_IN", "VirtualSession", sessionId, session.getTitle());
        return toAttendanceResponse(attendance);
    }

    @Transactional
    public List<AttendanceResponse> markAttendance(Long sessionId, AttendanceBulkRequest request) {
        VirtualSession session = find(sessionId);
        activityService.requireManage(session.getActivity());
        List<AttendanceResponse> responses = new ArrayList<>();
        for (AttendanceEntry entry : request.entries()) {
            if (entry.youthId() == null) {
                throw new BusinessRuleException("YOUTH_REQUIRED", "Each attendance entry needs a participant.");
            }
            activityService.requireEnrolled(session.getActivity(), entry.youthId());
            YouthParticipant youth = userRepository.findById(entry.youthId())
                    .filter(YouthParticipant.class::isInstance)
                    .map(YouthParticipant.class::cast)
                    .orElseThrow(() -> new NotFoundException("PARTICIPANT_NOT_FOUND", "Participant not found."));
            Attendance attendance = attendanceRepository
                    .findBySessionSessionIdAndYouthUserId(sessionId, entry.youthId())
                    .orElseGet(() -> new Attendance(session, youth, entry.status()));
            attendance.mark(entry.status());
            responses.add(toAttendanceResponse(attendanceRepository.save(attendance)));
        }
        auditService.record(currentUser(), "ATTENDANCE_MARKED", "VirtualSession", sessionId,
                responses.size() + " records");
        return responses;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> attendance(Long sessionId) {
        VirtualSession session = find(sessionId);
        activityService.requireManage(session.getActivity());
        return attendanceRepository.findBySessionSessionId(sessionId).stream().map(this::toAttendanceResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> myAttendance() {
        return attendanceRepository.findByYouthUserId(securityUtils.currentUserId()).stream()
                .map(this::toAttendanceResponse).toList();
    }

    public VirtualSession find(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND", "Session not found."));
    }

    public SessionResponse toResponse(VirtualSession session) {
        return new SessionResponse(session.getSessionId(), session.getActivity().getActivityId(), session.getTitle(),
                session.getScheduledStart(), session.getDurationMinutes(), session.getMeetingLink(),
                session.getStatus(), session.isCheckInOpen(OffsetDateTime.now()));
    }

    public AttendanceResponse toAttendanceResponse(Attendance attendance) {
        return new AttendanceResponse(attendance.getAttendanceId(), attendance.getSession().getSessionId(),
                attendance.getYouth().getUserId(), attendance.getYouth().getName(), attendance.getStatus(),
                attendance.getMarkedAt());
    }

    private YouthParticipant currentParticipant() {
        User user = currentUser();
        if (!(user instanceof YouthParticipant participant)) {
            throw new ForbiddenException("PARTICIPANT_ONLY", "Only youth participants can check in.");
        }
        return participant;
    }

    private void notifyEnrolled(LearningActivity activity, String message) {
        enrollmentRepository.findByActivityActivityIdAndStatus(activity.getActivityId(), EnrollmentStatus.ENROLLED)
                .forEach(enrollment -> notificationService.notify(enrollment.getYouth(), message,
                        "/exchanges/" + activity.getActivityId(), NotificationType.IN_APP));
    }

    private User currentUser() {
        return userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
    }
}
