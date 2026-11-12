package com.yep.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.AttendanceStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.SessionStatus;
import com.yep.domain.learning.Assessment;
import com.yep.domain.learning.Certificate;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.CertificateResponse;
import com.yep.dto.LearningDtos.ProgressResponse;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.AssessmentRepository;
import com.yep.repository.AttendanceRepository;
import com.yep.repository.CertificateRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.UserRepository;
import com.yep.repository.VirtualSessionRepository;
import com.yep.security.SecurityUtils;

@Service
public class ProgressService {

    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final VirtualSessionRepository sessionRepository;
    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final AssessmentService assessmentService;
    private final SecurityUtils securityUtils;

    public ProgressService(EnrollmentRepository enrollmentRepository, AssessmentRepository assessmentRepository,
            AttendanceRepository attendanceRepository, VirtualSessionRepository sessionRepository,
            CertificateRepository certificateRepository, UserRepository userRepository, ActivityService activityService,
            AssessmentService assessmentService, SecurityUtils securityUtils) {
        this.enrollmentRepository = enrollmentRepository;
        this.assessmentRepository = assessmentRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionRepository = sessionRepository;
        this.certificateRepository = certificateRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.assessmentService = assessmentService;
        this.securityUtils = securityUtils;
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> myProgress() {
        Long youthId = securityUtils.currentUserId();
        return enrollmentRepository.findByYouthUserId(youthId).stream()
                .map(enrollment -> progressFor(enrollment.getActivity().getActivityId(), youthId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressResponse progress(Long activityId) {
        return progressFor(activityId, securityUtils.currentUserId());
    }

    @Transactional(readOnly = true)
    public ProgressResponse progressFor(Long activityId, Long youthId) {
        LearningActivity activity = activityService.find(activityId);
        User actor = userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
        boolean manager = actor.getRole() == Role.ADMIN
                || (actor.getRole() == Role.FACILITATOR && activity.getFacilitator().getUserId().equals(actor.getUserId()));
        if (!manager && !actor.getUserId().equals(youthId)) {
            throw new ForbiddenException("FORBIDDEN", "You can only view your own progress.");
        }
        Enrollment enrollment = enrollmentRepository.findByYouthUserIdAndActivityActivityId(youthId, activityId)
                .orElseThrow(() -> new NotFoundException("ENROLLMENT_NOT_FOUND", "Enrollment not found."));
        YouthParticipant youth = enrollment.getYouth();

        // Attendance % = (PRESENT + EXCUSED) over COMPLETED sessions only.
        // When the activity has no completed sessions the attendance check is skipped (null).
        long completedSessions = sessionRepository.countByActivityActivityIdAndStatus(activityId,
                SessionStatus.COMPLETED);
        long attended = attendanceRepository.countByYouthUserIdAndSessionActivityActivityIdAndSessionStatusInAndStatusIn(
                youthId, activityId, List.of(SessionStatus.COMPLETED),
                List.of(AttendanceStatus.PRESENT, AttendanceStatus.EXCUSED));
        Double attendancePercent = completedSessions == 0 ? null
                : round(Math.min(100.0, (attended * 100.0) / completedSessions));

        List<Assessment> assessments = assessmentRepository.findByYouthUserIdAndActivityActivityId(youthId, activityId);
        List<Assessment> graded = assessments.stream().filter(Assessment::isGraded).toList();
        double average = graded.stream().mapToDouble(Assessment::percentage).average().orElse(0.0);
        boolean passedAssessments = !graded.isEmpty() && average >= activity.getPassMark();
        boolean passedAttendance = attendancePercent == null
                || attendancePercent >= activity.getMinAttendancePercent();
        boolean completed = enrollment.getStatus() == com.yep.domain.enums.EnrollmentStatus.COMPLETED
                || (passedAssessments && passedAttendance);

        List<CertificateResponse> certificates = certificateRepository.findByYouthUserId(youthId).stream()
                .filter(certificate -> certificate.getActivity() != null
                        && certificate.getActivity().getActivityId().equals(activityId))
                .map(this::toCertificateResponse)
                .toList();

        return new ProgressResponse(activityId, activity.getTitle(), enrollment.getStatus(), completedSessions, attended,
                attendancePercent, graded.size(), round(average), activity.getPassMark(),
                activity.getMinAttendancePercent(), passedAssessments, passedAttendance, completed,
                youth.isLevelTwoEligible(),
                assessments.stream().map(assessmentService::toResponse).toList(), certificates);
    }

    public CertificateResponse toCertificateResponse(Certificate certificate) {
        return new CertificateResponse(certificate.getCertificateId(), certificate.getCertificateNumber(),
                certificate.getYouth().getUserId(), certificate.getYouth().getName(),
                certificate.getActivity() == null ? null : certificate.getActivity().getActivityId(),
                certificate.getActivity() == null ? null : certificate.getActivity().getTitle(),
                certificate.getAchievement(), certificate.getIssueDate(), certificate.isRevoked());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
