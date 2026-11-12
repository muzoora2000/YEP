package com.yep.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.SessionStatus;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.learning.VirtualSession;
import com.yep.domain.project.Milestone;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.MilestoneRepository;
import com.yep.repository.PasswordResetTokenRepository;
import com.yep.repository.VirtualSessionRepository;

/**
 * Template Method pattern: each scheduled job follows the same skeleton —
 * fetch candidates → process each → log outcome.
 * Spring @Scheduled drives the cron; @Transactional keeps DB operations atomic.
 */
@Service
public class ScheduledJobsService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobsService.class);

    // All dependencies injected via constructor — Encapsulation: implementation hidden
    private final VirtualSessionRepository sessionRepository;
    private final MilestoneRepository milestoneRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    public ScheduledJobsService(VirtualSessionRepository sessionRepository,
            MilestoneRepository milestoneRepository,
            PasswordResetTokenRepository tokenRepository,
            EnrollmentRepository enrollmentRepository,
            NotificationService notificationService) {
        this.sessionRepository = sessionRepository;
        this.milestoneRepository = milestoneRepository;
        this.tokenRepository = tokenRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
    }

    /**
     * Hourly: notify enrolled participants of sessions starting in the next 60 minutes.
     * Template step: find upcoming sessions → find enrolled users → notify → log.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendSessionReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime horizon = now.plusHours(1);
        List<VirtualSession> upcoming = sessionRepository
                .findByStatusAndScheduledStartBetween(SessionStatus.SCHEDULED, now, horizon);
        for (VirtualSession session : upcoming) {
            Long activityId = session.getActivity().getActivityId();
            String msg = "Session starting soon: \"" + session.getTitle() + "\". "
                    + "Join via: " + session.getMeetingLink();
            String link = "/exchanges/" + activityId;
            List<Enrollment> enrolled = enrollmentRepository
                    .findByActivityActivityIdAndStatus(activityId, EnrollmentStatus.ENROLLED);
            for (Enrollment enrollment : enrolled) {
                // Polymorphism: notificationService.notify() dispatches to EmailNotification/InAppNotification
                notificationService.notifyUser(
                        enrollment.getYouth().getUserId(), msg, link, NotificationType.IN_APP);
            }
        }
        log.info("[Scheduler] Session reminders sent for {} upcoming sessions.", upcoming.size());
    }

    /**
     * Daily at 07:00 UTC: warn team members when a milestone is due in exactly 2 days.
     * Template step: find near-due milestones → notify all team members → log.
     */
    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void sendMilestoneReminders() {
        LocalDate twoDaysOut = LocalDate.now().plusDays(2);
        List<Milestone> dueSoon = milestoneRepository
                .findByStatusNotAndDueDate(MilestoneStatus.DONE, twoDaysOut);
        for (Milestone milestone : dueSoon) {
            String msg = "Milestone \"" + milestone.getTitle() + "\" is due in 2 days. "
                    + "Update its progress in your impact project.";
            String link = "/projects/" + milestone.getProject().getProjectId();
            milestone.getProject().getTeam().getMembers().forEach(member ->
                notificationService.notifyUser(
                        member.getYouth().getUserId(), msg, link, NotificationType.IN_APP)
            );
        }
        log.info("[Scheduler] Milestone reminders sent for {} near-due milestones.", dueSoon.size());
    }

    /**
     * Daily at 03:00 UTC: delete expired and used password-reset tokens (security hygiene).
     * Template step: bulk-delete stale records → log the count removed.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanExpiredTokens() {
        long deleted = tokenRepository.deleteByExpiresAtBeforeOrUsedTrue(OffsetDateTime.now());
        log.info("[Scheduler] Cleaned {} expired/used password-reset tokens.", deleted);
    }
}
