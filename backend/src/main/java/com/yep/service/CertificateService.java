package com.yep.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.ActivityLevel;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.learning.Certificate;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.CertificateResponse;
import com.yep.dto.LearningDtos.ProgressResponse;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.CertificateRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

@Service
public class CertificateService implements CertificateIssuer {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String NUMBER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final CertificateRepository certificateRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ImpactProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ActivityService activityService;
    private final ProgressService progressService;
    private final CertificatePdfService pdfService;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public CertificateService(CertificateRepository certificateRepository, EnrollmentRepository enrollmentRepository,
            TeamMemberRepository teamMemberRepository, ImpactProjectRepository projectRepository,
            UserRepository userRepository, ActivityService activityService, ProgressService progressService,
            CertificatePdfService pdfService, SecurityUtils securityUtils, AuditService auditService,
            NotificationService notificationService) {
        this.certificateRepository = certificateRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.activityService = activityService;
        this.progressService = progressService;
        this.pdfService = pdfService;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    /** Facilitator/admin manual issuance with detailed business-rule errors. */
    @Transactional
    public CertificateResponse issue(Long activityId, Long youthId) {
        LearningActivity activity = activityService.find(activityId);
        activityService.requireManage(activity);
        if (certificateRepository.existsByYouthUserIdAndActivityActivityId(youthId, activityId)) {
            throw new ConflictException("CERTIFICATE_EXISTS",
                    "This participant already has a certificate for the activity.");
        }
        Enrollment enrollment = enrollmentRepository.findByYouthUserIdAndActivityActivityId(youthId, activityId)
                .orElseThrow(() -> new BusinessRuleException("ENROLLMENT_REQUIRED",
                        "The participant must be enrolled before a certificate can be issued."));
        YouthParticipant youth = enrollment.getYouth();
        ProgressResponse progress = progressService.progressFor(activityId, youthId);
        if (!progress.passedAssessments()) {
            throw new BusinessRuleException("ASSESSMENT_REQUIREMENT_NOT_MET",
                    "The participant has not met the assessment pass mark.");
        }
        if (!progress.passedAttendance()) {
            throw new BusinessRuleException("ATTENDANCE_REQUIREMENT_NOT_MET",
                    "The participant has not met the minimum attendance requirement.");
        }
        ImpactProject project = projectFor(youth, activity);
        if (activity.getLevel() == ActivityLevel.TWO && !isProjectComplete(project)) {
            throw new BusinessRuleException("PROJECT_REQUIREMENT_NOT_MET",
                    "The team project must be approved or completed before a Level Two certificate can be issued.");
        }
        Certificate certificate = issueCertificate(youth, activity, achievementText(activity, project));
        return progressService.toCertificateResponse(certificate);
    }

    /**
     * Core issuance used by the completion workflows. Creates the certificate, records
     * Level One eligibility, completes the enrollment, notifies and audits. The caller
     * is responsible for the eligibility decision (see {@link #isEligible}).
     */
    @Override
    @Transactional
    public Certificate issueCertificate(YouthParticipant youth, LearningActivity activity, String achievement) {
        if (certificateRepository.existsByYouthUserIdAndActivityActivityId(youth.getUserId(),
                activity.getActivityId())) {
            throw new ConflictException("CERTIFICATE_EXISTS",
                    "This participant already has a certificate for the activity.");
        }
        Certificate certificate = new Certificate(youth, nextNumber(), achievement);
        certificate.setActivity(activity);
        ImpactProject project = projectFor(youth, activity);
        if (project != null) {
            certificate.setProject(project);
        }
        certificate = certificateRepository.save(certificate);

        if (activity.getLevel() == ActivityLevel.ONE) {
            youth.setLevelTwoEligible(true);
            userRepository.save(youth);
        }
        enrollmentRepository.findByYouthUserIdAndActivityActivityId(youth.getUserId(), activity.getActivityId())
                .ifPresent(enrollment -> {
                    if (enrollment.getStatus() != EnrollmentStatus.COMPLETED) {
                        enrollment.completeEnrollment();
                        enrollmentRepository.save(enrollment);
                    }
                });
        notificationService.notify(youth,
                "Your certificate for " + activity.getTitle() + " is ready: " + certificate.getCertificateNumber(),
                "/certificates/" + certificate.getCertificateNumber(), NotificationType.IN_APP);
        auditService.record(currentUser(), "CERTIFICATE_ISSUED", "Certificate", certificate.getCertificateId(),
                certificate.getCertificateNumber());
        return certificate;
    }

    /** Base eligibility: an active (ENROLLED) participant without an existing certificate who met the requirements. */
    @Override
    @Transactional(readOnly = true)
    public boolean isEligible(YouthParticipant participant, LearningActivity activity) {
        Long youthId = participant.getUserId();
        Long activityId = activity.getActivityId();
        if (certificateRepository.existsByYouthUserIdAndActivityActivityId(youthId, activityId)) {
            return false;
        }
        Enrollment enrollment = enrollmentRepository.findByYouthUserIdAndActivityActivityId(youthId, activityId)
                .orElse(null);
        if (enrollment == null || enrollment.getStatus() != EnrollmentStatus.ENROLLED) {
            return false;
        }
        ProgressResponse progress = progressService.progressFor(activityId, youthId);
        return progress.passedAssessments() && progress.passedAttendance();
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> myCertificates() {
        return certificateRepository.findByYouthUserId(securityUtils.currentUserId()).stream()
                .map(progressService::toCertificateResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificateResponse verify(String certificateNumber) {
        return progressService.toCertificateResponse(findByNumber(certificateNumber));
    }

    @Transactional(readOnly = true)
    public byte[] pdf(String certificateNumber) {
        Certificate certificate = findByNumber(certificateNumber);
        if (!certificate.isValid()) {
            throw new BusinessRuleException("CERTIFICATE_REVOKED", "This certificate has been revoked.");
        }
        return pdfService.render(certificate);
    }

    /** Owner (or facilitator/admin) download by id: GET /api/v1/certificates/{id}/pdf. */
    @Transactional(readOnly = true)
    public byte[] pdfById(Long certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("CERTIFICATE_NOT_FOUND", "Certificate not found."));
        AuthenticatedUser actor = securityUtils.currentUser();
        boolean owner = certificate.getYouth().getUserId().equals(actor.userId());
        boolean admin = actor.role() == Role.ADMIN;
        boolean facilitator = actor.role() == Role.FACILITATOR && certificate.getActivity() != null
                && certificate.getActivity().getFacilitator().getUserId().equals(actor.userId());
        if (!owner && !admin && !facilitator) {
            throw new ForbiddenException("FORBIDDEN", "You cannot download this certificate.");
        }
        if (!certificate.isValid()) {
            throw new BusinessRuleException("CERTIFICATE_REVOKED", "This certificate has been revoked.");
        }
        return pdfService.render(certificate);
    }

    @Transactional
    public CertificateResponse revoke(Long certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new NotFoundException("CERTIFICATE_NOT_FOUND", "Certificate not found."));
        certificate.revoke();
        certificateRepository.save(certificate);
        auditService.record(currentUser(), "CERTIFICATE_REVOKED", "Certificate", certificateId,
                certificate.getCertificateNumber());
        return progressService.toCertificateResponse(certificate);
    }

    /** The team project a participant contributed to for the given activity, if any. */
    @Transactional(readOnly = true)
    public ImpactProject projectFor(YouthParticipant youth, LearningActivity activity) {
        return teamMemberRepository.findByYouthUserId(youth.getUserId()).stream()
                .filter(member -> member.getTeam().getActivity().getActivityId().equals(activity.getActivityId()))
                .findFirst()
                .flatMap(member -> projectRepository.findByTeamTeamId(member.getTeam().getTeamId()))
                .orElse(null);
    }

    /** Achievement text; Level Two names the team project when one exists. */
    public String achievementText(LearningActivity activity, ImpactProject project) {
        if (activity.getLevel() == ActivityLevel.ONE) {
            return "Completion of " + activity.getTitle() + " (Level One)";
        }
        if (project != null) {
            return "Completion of " + activity.getTitle() + " (Level Two) - project: " + project.getTitle();
        }
        return "Completion of " + activity.getTitle() + " (Level Two)";
    }

    public boolean isProjectComplete(ImpactProject project) {
        return project != null
                && (project.getStatus() == ProjectStatus.APPROVED || project.getStatus() == ProjectStatus.COMPLETED);
    }

    private Certificate findByNumber(String certificateNumber) {
        return certificateRepository.findByCertificateNumberIgnoreCase(certificateNumber)
                .orElseThrow(() -> new NotFoundException("CERTIFICATE_NOT_FOUND", "Certificate not found."));
    }

    /** Format: YEP-YYYY-XXXXXX where XXXXXX is 6 random uppercase letters/digits. */
    private String nextNumber() {
        String number;
        do {
            StringBuilder builder = new StringBuilder("YEP-").append(LocalDate.now().getYear()).append('-');
            for (int i = 0; i < 6; i++) {
                builder.append(NUMBER_CHARS.charAt(RANDOM.nextInt(NUMBER_CHARS.length())));
            }
            number = builder.toString();
        } while (certificateRepository.existsByCertificateNumberIgnoreCase(number));
        return number;
    }

    private User currentUser() {
        return userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Account not found."));
    }
}
