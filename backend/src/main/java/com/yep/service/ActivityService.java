package com.yep.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.activity.LevelOneActivity;
import com.yep.domain.activity.LevelTwoActivity;
import com.yep.domain.enums.ActivityLevel;
import com.yep.domain.enums.ActivityStatus;
import com.yep.domain.enums.AssessmentStatus;
import com.yep.domain.enums.AttendanceStatus;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.NotificationType;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.SessionStatus;
import com.yep.domain.learning.Assessment;
import com.yep.domain.learning.AssessmentTask;
import com.yep.domain.learning.DiscussionThread;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.PartnerOrganisation;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.ActivityResponse;
import com.yep.dto.LearningDtos.CreateActivityRequest;
import com.yep.dto.LearningDtos.EnrollmentDecisionRequest;
import com.yep.dto.LearningDtos.EnrollmentResponse;
import com.yep.dto.LearningDtos.MyResultsResponse;
import com.yep.dto.LearningDtos.ParticipantRow;
import com.yep.dto.LearningDtos.TaskResult;
import com.yep.dto.LearningDtos.UpdateActivityRequest;
import com.yep.dto.PageResponse;
import com.yep.exception.BadRequestException;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.AssessmentRepository;
import com.yep.repository.AssessmentTaskRepository;
import com.yep.repository.AttendanceRepository;
import com.yep.repository.DiscussionPostRepository;
import com.yep.repository.DiscussionThreadRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.FacilitatorRepository;
import com.yep.repository.LearningActivityRepository;
import com.yep.repository.PartnerOrganisationRepository;
import com.yep.repository.UserRepository;
import com.yep.repository.VirtualSessionRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

import jakarta.persistence.criteria.Predicate;

@Service
public class ActivityService {

    private static final String GENERAL_THREAD_TOPIC = "General";

    private final LearningActivityRepository activityRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final FacilitatorRepository facilitatorRepository;
    private final PartnerOrganisationRepository partnerRepository;
    private final UserRepository userRepository;
    private final DiscussionThreadRepository threadRepository;
    private final VirtualSessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentTaskRepository assessmentTaskRepository;
    private final DiscussionPostRepository discussionPostRepository;
    private final SecurityUtils securityUtils;
    private final AuditService auditService;
    private final SettingsService settingsService;
    private final NotificationService notificationService;

    public ActivityService(LearningActivityRepository activityRepository, EnrollmentRepository enrollmentRepository,
            FacilitatorRepository facilitatorRepository, PartnerOrganisationRepository partnerRepository,
            UserRepository userRepository, DiscussionThreadRepository threadRepository,
            VirtualSessionRepository sessionRepository, AttendanceRepository attendanceRepository,
            AssessmentRepository assessmentRepository, AssessmentTaskRepository assessmentTaskRepository,
            DiscussionPostRepository discussionPostRepository, SecurityUtils securityUtils, AuditService auditService,
            SettingsService settingsService, NotificationService notificationService) {
        this.activityRepository = activityRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.facilitatorRepository = facilitatorRepository;
        this.partnerRepository = partnerRepository;
        this.userRepository = userRepository;
        this.threadRepository = threadRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentTaskRepository = assessmentTaskRepository;
        this.discussionPostRepository = discussionPostRepository;
        this.securityUtils = securityUtils;
        this.auditService = auditService;
        this.settingsService = settingsService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ActivityResponse create(CreateActivityRequest request) {
        AuthenticatedUser actor = securityUtils.currentUser();
        Facilitator facilitator;
        if (actor.role() == Role.FACILITATOR) {
            facilitator = facilitatorRepository.findById(actor.userId())
                    .orElseThrow(() -> new ForbiddenException("FACILITATOR_ONLY", "Facilitator account required."));
        } else if (actor.role() == Role.ADMIN) {
            if (request.facilitatorId() == null) {
                throw new BadRequestException("FACILITATOR_REQUIRED", "Select a facilitator for this activity.");
            }
            facilitator = facilitatorRepository.findById(request.facilitatorId())
                    .orElseThrow(() -> new NotFoundException("FACILITATOR_NOT_FOUND", "Facilitator not found."));
        } else {
            throw new ForbiddenException("FORBIDDEN", "Only facilitators and administrators create activities.");
        }

        LearningActivity activity = request.level() == ActivityLevel.ONE
                ? new LevelOneActivity()
                : new LevelTwoActivity();
        activity.setFacilitator(facilitator);
        activity.setTitle(request.title().trim());
        activity.setDescription(trimToNull(request.description()));
        activity.setStartDate(request.startDate());
        activity.setEndDate(request.endDate());
        activity.setCategory(trimToNull(request.category()));
        activity.setCapacity(request.capacity() != null ? request.capacity()
                : (request.level() == ActivityLevel.TWO
                        ? settingsService.intValue(SettingsService.LEVEL_TWO_CAPACITY, 30)
                        : null));
        activity.setPassMark(request.passMark() != null ? request.passMark()
                : settingsService.intValue(SettingsService.DEFAULT_PASS_MARK, 50));
        activity.setMinAttendancePercent(request.minAttendancePercent() != null ? request.minAttendancePercent()
                : settingsService.intValue(SettingsService.DEFAULT_ATTENDANCE_THRESHOLD, 75));
        activity.setMinTeamSize(request.minTeamSize() != null ? request.minTeamSize() : 3);
        activity.setMaxTeamSize(request.maxTeamSize() != null ? request.maxTeamSize() : 5);
        if (request.partnerId() != null) {
            activity.setPartner(findPartner(request.partnerId()));
        }
        validateActivity(activity);
        activity = activityRepository.save(activity);
        auditService.record(actorUser(actor), "ACTIVITY_CREATED", "LearningActivity", activity.getActivityId(),
                activity.getTitle());
        return toResponse(activity);
    }

    @Transactional
    public ActivityResponse update(Long activityId, UpdateActivityRequest request) {
        LearningActivity activity = find(activityId);
        requireManage(activity);
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BadRequestException("VALIDATION_FAILED", "Title cannot be blank.");
            }
            activity.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            activity.setDescription(trimToNull(request.description()));
        }
        if (request.startDate() != null) {
            activity.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            activity.setEndDate(request.endDate());
        }
        if (request.category() != null) {
            activity.setCategory(trimToNull(request.category()));
        }
        if (request.capacity() != null) {
            long enrolled = enrollmentRepository.countByActivityActivityIdAndStatus(activityId,
                    EnrollmentStatus.ENROLLED);
            if (request.capacity() < enrolled) {
                throw new BusinessRuleException("CAPACITY_TOO_LOW",
                        "Capacity cannot be lower than the number of enrolled participants.");
            }
            activity.setCapacity(request.capacity());
        }
        if (request.partnerId() != null) {
            activity.setPartner(findPartner(request.partnerId()));
        }
        if (request.passMark() != null) {
            activity.setPassMark(request.passMark());
        }
        if (request.minAttendancePercent() != null) {
            activity.setMinAttendancePercent(request.minAttendancePercent());
        }
        if (request.minTeamSize() != null) {
            activity.setMinTeamSize(request.minTeamSize());
        }
        if (request.maxTeamSize() != null) {
            activity.setMaxTeamSize(request.maxTeamSize());
        }
        validateActivity(activity);
        activityRepository.save(activity);
        auditService.record(currentActorUser(), "ACTIVITY_UPDATED", "LearningActivity", activityId, activity.getTitle());
        return toResponse(activity);
    }

    @Transactional
    public ActivityResponse changeStatus(Long activityId, ActivityStatus status) {
        LearningActivity activity = find(activityId);
        requireManage(activity);
        // Encapsulation of the lifecycle rule: statuses move forward only DRAFT -> PUBLISHED -> CLOSED -> COMPLETED.
        if (status.ordinal() != activity.getStatus().ordinal() + 1) {
            throw new BusinessRuleException("ILLEGAL_STATUS_TRANSITION",
                    "Activities move forward only: DRAFT -> PUBLISHED -> CLOSED -> COMPLETED.");
        }
        activity.setStatus(status);
        activityRepository.save(activity);
        if (status == ActivityStatus.PUBLISHED) {
            createGeneralThreadIfMissing(activity);
        }
        auditService.record(currentActorUser(), "ACTIVITY_STATUS_CHANGED", "LearningActivity", activityId,
                "Status set to " + status);
        return toResponse(activity);
    }

    /** Business rule: every published activity has a default activity-wide "General" discussion thread. */
    private void createGeneralThreadIfMissing(LearningActivity activity) {
        boolean exists = threadRepository
                .findFirstByActivityActivityIdAndTeamIsNullAndTopicIgnoreCase(activity.getActivityId(),
                        GENERAL_THREAD_TOPIC)
                .isPresent();
        if (!exists) {
            threadRepository.save(new DiscussionThread(activity, GENERAL_THREAD_TOPIC));
        }
    }

    /**
     * Public activity listing with optional filters. Visibility: participants and
     * anonymous callers see only PUBLISHED activities; facilitators additionally see
     * their own non-published ones; administrators see everything.
     */
    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> list(ActivityLevel level, String category, String query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        AuthenticatedUser actor = securityUtils.currentUserOrNull();
        Specification<LearningActivity> specification = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (level != null) {
                // Polymorphic filter on the SINGLE_TABLE discriminator without instanceof checks.
                predicates.add(criteriaBuilder.equal(root.type(),
                        level == ActivityLevel.ONE ? LevelOneActivity.class : LevelTwoActivity.class));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")),
                        category.trim().toLowerCase()));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), like),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), like)));
            }
            Predicate visibility = visibilityPredicate(root, criteriaBuilder, actor);
            if (visibility != null) {
                predicates.add(visibility);
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        Page<LearningActivity> result = activityRepository.findAll(specification,
                PageRequest.of(safePage, safeSize, Sort.by("startDate").ascending().and(Sort.by("activityId"))));
        return PageResponse.from(result.map(this::toResponse));
    }

    private Predicate visibilityPredicate(jakarta.persistence.criteria.Root<LearningActivity> root,
            jakarta.persistence.criteria.CriteriaBuilder criteriaBuilder, AuthenticatedUser actor) {
        if (actor == null || actor.role() == Role.PARTICIPANT || actor.role() == Role.MENTOR
                || actor.role() == Role.PARTNER) {
            return criteriaBuilder.equal(root.get("status"), ActivityStatus.PUBLISHED);
        }
        if (actor.role() == Role.FACILITATOR) {
            return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("status"), ActivityStatus.PUBLISHED),
                    criteriaBuilder.equal(root.get("facilitator").get("userId"), actor.userId()));
        }
        return null;
    }

    @Transactional(readOnly = true)
    public ActivityResponse publicGet(Long activityId) {
        LearningActivity activity = find(activityId);
        if (!activity.isPublished()) {
            throw new NotFoundException("ACTIVITY_NOT_FOUND", "Activity not found.");
        }
        return toResponse(activity);
    }

    @Transactional(readOnly = true)
    public ActivityResponse manageGet(Long activityId) {
        LearningActivity activity = find(activityId);
        requireManage(activity);
        return toResponse(activity);
    }

    @Transactional
    public EnrollmentResponse enroll(Long activityId) {
        YouthParticipant youth = currentParticipant();
        LearningActivity activity = find(activityId);
        if (activity.getLevel() == ActivityLevel.TWO) {
            throw new BusinessRuleException("LEVEL_TWO_REQUIRES_APPLICATION",
                    "Level Two activities require an application that the facilitator reviews.");
        }
        if (!activity.isPublished()) {
            throw new BusinessRuleException("ACTIVITY_NOT_OPEN", "This activity is not open for enrollment.");
        }
        if (activity.getStatus() == ActivityStatus.CLOSED || activity.getStatus() == ActivityStatus.COMPLETED) {
            throw new BusinessRuleException("ACTIVITY_CLOSED", "This activity is no longer accepting enrollments.");
        }
        if (enrollmentRepository.existsByYouthUserIdAndActivityActivityId(youth.getUserId(), activityId)) {
            throw new ConflictException("ALREADY_ENROLLED", "You already have an enrollment for this activity.");
        }
        Enrollment enrollment = new Enrollment(youth, activity);
        long enrolled = enrollmentRepository.countByActivityActivityIdAndStatus(activityId, EnrollmentStatus.ENROLLED);
        if (activity.hasCapacity((int) enrolled)) {
            enrollment.confirmEnrollment();
        } else {
            enrollment.waitlistEnrollment();
        }
        enrollment = enrollmentRepository.save(enrollment);
        String enrollmentMessage = enrollment.getStatus() == EnrollmentStatus.ENROLLED
                ? "You are enrolled in " + activity.getTitle() + "."
                : "You are on the waitlist for " + activity.getTitle() + ".";
        notificationService.notify(youth, enrollmentMessage, "/exchanges/" + activityId, NotificationType.IN_APP);
        auditService.record(youth, "ACTIVITY_ENROLLED", "Enrollment", enrollment.getEnrollmentId(),
                activity.getTitle() + " - " + enrollment.getStatus());
        return toEnrollmentResponse(enrollment);
    }

    /**
     * Level Two application flow: participants apply (APPLIED, or WAITLISTED when
     * the activity is already full) and the facilitator selects or rejects them.
     */
    @Transactional
    public EnrollmentResponse apply(Long activityId) {
        YouthParticipant youth = currentParticipant();
        LearningActivity activity = find(activityId);
        if (activity.getLevel() != ActivityLevel.TWO) {
            throw new BusinessRuleException("LEVEL_ONE_DIRECT_ENROLL",
                    "Level One activities use direct enrollment.");
        }
        if (!activity.isPublished()) {
            throw new BusinessRuleException("ACTIVITY_NOT_OPEN", "This activity is not open for applications.");
        }
        if (activity.getStatus() == ActivityStatus.CLOSED || activity.getStatus() == ActivityStatus.COMPLETED) {
            throw new BusinessRuleException("ACTIVITY_CLOSED", "This activity is no longer accepting applications.");
        }
        if (!youth.isLevelTwoEligible()) {
            throw new BusinessRuleException("NOT_ELIGIBLE_LEVEL_TWO",
                    "Level Two is available to selected participants who completed Level One.");
        }
        if (enrollmentRepository.existsByYouthUserIdAndActivityActivityId(youth.getUserId(), activityId)) {
            throw new ConflictException("ALREADY_ENROLLED", "You already have an enrollment for this activity.");
        }
        Enrollment enrollment = new Enrollment(youth, activity);
        long enrolled = enrollmentRepository.countByActivityActivityIdAndStatus(activityId, EnrollmentStatus.ENROLLED);
        if (!activity.hasCapacity((int) enrolled)) {
            enrollment.waitlistEnrollment();
        }
        enrollment = enrollmentRepository.save(enrollment);
        String message = enrollment.getStatus() == EnrollmentStatus.WAITLISTED
                ? "You are on the waitlist for " + activity.getTitle() + "."
                : "Your application for " + activity.getTitle() + " was received and is awaiting review.";
        notificationService.notify(youth, message, "/exchanges/" + activityId, NotificationType.IN_APP);
        auditService.record(youth, "ACTIVITY_APPLIED", "Enrollment", enrollment.getEnrollmentId(),
                activity.getTitle() + " - " + enrollment.getStatus());
        return toEnrollmentResponse(enrollment);
    }

    /** Facilitator/admin decision on a Level Two application: SELECT (alias ENROLL/ACCEPT) or REJECT. */
    @Transactional
    public EnrollmentResponse decide(Long enrollmentId, EnrollmentDecisionRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("ENROLLMENT_NOT_FOUND", "Enrollment not found."));
        LearningActivity activity = enrollment.getActivity();
        requireManage(activity);
        if (enrollment.getStatus() != EnrollmentStatus.APPLIED
                && enrollment.getStatus() != EnrollmentStatus.WAITLISTED) {
            throw new BusinessRuleException("ENROLLMENT_NOT_DECIDABLE",
                    "Only pending applications or waitlisted enrollments can be decided.");
        }
        String decision = request.decision() == null ? "" : request.decision().trim().toUpperCase();
        switch (decision) {
            case "SELECT", "ENROLL", "ACCEPT" -> {
                long enrolled = enrollmentRepository.countByActivityActivityIdAndStatus(activity.getActivityId(),
                        EnrollmentStatus.ENROLLED);
                if (!activity.hasCapacity((int) enrolled)) {
                    enrollment.waitlistEnrollment();
                } else {
                    enrollment.confirmEnrollment();
                }
            }
            case "REJECT" -> enrollment.rejectEnrollment();
            default -> throw new BadRequestException("INVALID_DECISION",
                    "Decision must be SELECT or REJECT.");
        }
        enrollmentRepository.save(enrollment);
        String message = switch (enrollment.getStatus()) {
            case ENROLLED -> "You have been selected for " + activity.getTitle() + ".";
            case WAITLISTED -> "You are on the waitlist for " + activity.getTitle() + ".";
            case REJECTED -> "Your application for " + activity.getTitle() + " was not selected.";
            default -> "Your enrollment status for " + activity.getTitle() + " is now " + enrollment.getStatus() + ".";
        };
        notificationService.notify(enrollment.getYouth(), message,
                "/exchanges/" + activity.getActivityId(), NotificationType.IN_APP);
        auditService.record(currentActorUser(), "ENROLLMENT_DECISION", "Enrollment", enrollmentId,
                "Decision " + decision + " -> " + enrollment.getStatus());
        return toEnrollmentResponse(enrollment);
    }

    @Transactional
    public EnrollmentResponse cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new NotFoundException("ENROLLMENT_NOT_FOUND", "Enrollment not found."));
        AuthenticatedUser actor = securityUtils.currentUser();
        boolean owner = enrollment.getYouth().getUserId().equals(actor.userId());
        if (!owner && actor.role() != Role.ADMIN) {
            requireManage(enrollment.getActivity());
        }
        LearningActivity activity = enrollment.getActivity();
        if (activity.getEndDate() != null && LocalDate.now().isAfter(activity.getEndDate())) {
            throw new BusinessRuleException("ACTIVITY_ENDED",
                    "This activity has already ended; its enrollments can no longer be cancelled.");
        }
        boolean wasEnrolled = enrollment.isActive();
        enrollment.cancelEnrollment();
        enrollmentRepository.save(enrollment);
        notificationService.notify(enrollment.getYouth(),
                "Your enrollment in " + activity.getTitle() + " was cancelled.",
                "/exchanges/" + activity.getActivityId(), NotificationType.IN_APP);
        if (wasEnrolled) {
            promoteWaitlist(activity);
        }
        auditService.record(actorUser(actor), "ENROLLMENT_CANCELLED", "Enrollment", enrollmentId,
                activity.getTitle());
        return toEnrollmentResponse(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> myEnrollments() {
        return enrollmentRepository.findByYouthUserId(securityUtils.currentUserId()).stream()
                .map(this::toEnrollmentResponse)
                .toList();
    }

    /** Facilitator/admin: activities owned by the current facilitator (all activities for admins). */
    @Transactional(readOnly = true)
    public PageResponse<ActivityResponse> listMine(int page, int size) {
        AuthenticatedUser actor = securityUtils.currentUser();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(safePage, safeSize,
                Sort.by("startDate").descending().and(Sort.by("activityId").descending()));
        if (actor.role() == Role.ADMIN) {
            return PageResponse.from(activityRepository.findAll(pageable).map(this::toResponse));
        }
        return PageResponse.from(
                activityRepository.findByFacilitatorUserId(actor.userId(), pageable).map(this::toResponse));
    }

    /**
     * Facilitator roster: each enrollment with nullable attendance % (over COMPLETED
     * sessions), nullable results % (graded assessments) and the last activity timestamp.
     */
    @Transactional(readOnly = true)
    public List<ParticipantRow> participants(Long activityId) {
        LearningActivity activity = find(activityId);
        requireManage(activity);
        long completedSessions = sessionRepository.countByActivityActivityIdAndStatus(activityId,
                SessionStatus.COMPLETED);
        return enrollmentRepository.findByActivityActivityId(activityId).stream()
                .map(enrollment -> {
                    Long youthId = enrollment.getYouth().getUserId();
                    return new ParticipantRow(youthId, enrollment.getYouth().getName(),
                            enrollment.getYouth().getCountry(), enrollment.getStatus(),
                            attendancePercent(youthId, activityId, completedSessions),
                            resultsPercent(youthId, activityId),
                            lastActivityAt(enrollment, youthId, activityId));
                })
                .toList();
    }

    /** Participant-facing results overview for every activity the current youth is enrolled in. */
    @Transactional(readOnly = true)
    public List<MyResultsResponse> meResults() {
        Long youthId = securityUtils.currentUserId();
        return enrollmentRepository.findByYouthUserId(youthId).stream()
                .map(enrollment -> resultsFor(enrollment.getActivity().getActivityId(), youthId))
                .toList();
    }

    /** Marks an activity COMPLETED. The completion workflow then issues eligible certificates. */
    @Transactional
    public void markCompleted(LearningActivity activity) {
        if (activity.getStatus() == ActivityStatus.COMPLETED) {
            throw new BusinessRuleException("ACTIVITY_ALREADY_COMPLETED", "This activity is already completed.");
        }
        activity.setStatus(ActivityStatus.COMPLETED);
        activityRepository.save(activity);
        auditService.record(currentActorUser(), "ACTIVITY_COMPLETED", "LearningActivity", activity.getActivityId(),
                activity.getTitle());
    }

    private Double attendancePercent(Long youthId, Long activityId, long completedSessions) {
        if (completedSessions == 0) {
            return null;
        }
        long attended = attendanceRepository
                .countByYouthUserIdAndSessionActivityActivityIdAndSessionStatusInAndStatusIn(youthId, activityId,
                        List.of(SessionStatus.COMPLETED), List.of(AttendanceStatus.PRESENT, AttendanceStatus.EXCUSED));
        return round(Math.min(100.0, (attended * 100.0) / completedSessions));
    }

    private Double resultsPercent(Long youthId, Long activityId) {
        List<Assessment> graded = assessmentRepository.findByYouthUserIdAndActivityActivityId(youthId, activityId)
                .stream().filter(Assessment::isGraded).toList();
        if (graded.isEmpty()) {
            return null;
        }
        return round(graded.stream().mapToDouble(Assessment::percentage).average().orElse(0.0));
    }

    private OffsetDateTime lastActivityAt(Enrollment enrollment, Long youthId, Long activityId) {
        OffsetDateTime last = enrollment.getEnrollmentDate();
        OffsetDateTime submission = assessmentRepository.findLastSubmissionDate(youthId, activityId).orElse(null);
        OffsetDateTime post = discussionPostRepository.findLastPostDate(youthId, activityId).orElse(null);
        if (submission != null && (last == null || submission.isAfter(last))) {
            last = submission;
        }
        if (post != null && (last == null || post.isAfter(last))) {
            last = post;
        }
        return last;
    }

    private MyResultsResponse resultsFor(Long activityId, Long youthId) {
        LearningActivity activity = find(activityId);
        List<AssessmentTask> tasks = assessmentTaskRepository
                .findByActivityActivityIdAndPublishedTrueOrderByDueDateAsc(activityId);
        List<TaskResult> taskResults = new ArrayList<>();
        int gradedCount = 0;
        double sum = 0.0;
        for (AssessmentTask task : tasks) {
            Assessment assessment = assessmentRepository.findByTaskTaskIdAndYouthUserId(task.getTaskId(), youthId)
                    .orElse(null);
            Integer score = assessment == null ? null : assessment.getScore();
            Integer maxScore = assessment == null ? null : assessment.getMaxScore();
            Double percentage = assessment != null && assessment.isGraded() ? round(assessment.percentage()) : null;
            AssessmentStatus status = assessment == null ? null : assessment.getStatus();
            if (percentage != null) {
                gradedCount++;
                sum += percentage;
            }
            taskResults.add(new TaskResult(task.getTaskId(), task.getTitle(), score, maxScore, percentage, status));
        }
        Double overall = gradedCount == 0 ? null : round(sum / gradedCount);
        return new MyResultsResponse(activityId, activity.getTitle(), overall, gradedCount, tasks.size(), taskResults);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public LearningActivity find(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new NotFoundException("ACTIVITY_NOT_FOUND", "Activity not found."));
    }

    @Transactional(readOnly = true)
    public LearningActivity findForParticipant(Long activityId) {
        LearningActivity activity = find(activityId);
        if (!activity.isPublished()) {
            throw new NotFoundException("ACTIVITY_NOT_FOUND", "Activity not found.");
        }
        return activity;
    }

    public void requireManage(LearningActivity activity) {
        AuthenticatedUser actor = securityUtils.currentUser();
        if (actor.role() == Role.ADMIN) {
            return;
        }
        if (actor.role() == Role.FACILITATOR
                && activity.getFacilitator().getUserId().equals(actor.userId())) {
            return;
        }
        throw new ForbiddenException("FORBIDDEN", "Only the assigned facilitator or an administrator can do that.");
    }

    public void requireEnrolled(LearningActivity activity, Long youthId) {
        enrollmentRepository.findByYouthUserIdAndActivityActivityId(youthId, activity.getActivityId())
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED
                        || enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .orElseThrow(() -> new ForbiddenException("ENROLLMENT_REQUIRED",
                        "Enroll in this activity to access this content."));
    }

    public ActivityResponse toResponse(LearningActivity activity) {
        long enrolled = enrollmentRepository.countByActivityActivityIdAndStatus(activity.getActivityId(),
                EnrollmentStatus.ENROLLED);
        long waitlisted = enrollmentRepository.countByActivityActivityIdAndStatus(activity.getActivityId(),
                EnrollmentStatus.WAITLISTED);
        PartnerOrganisation partner = activity.getPartner();
        return new ActivityResponse(activity.getActivityId(), activity.getLevel(), activity.getTitle(),
                activity.getDescription(), activity.getStartDate(), activity.getEndDate(), activity.getCategory(),
                activity.getCapacity(), activity.getStatus(), activity.getPassMark(),
                activity.getMinAttendancePercent(), activity.getMinTeamSize(), activity.getMaxTeamSize(),
                activity.getFacilitator().getUserId(), activity.getFacilitator().getName(),
                partner == null ? null : partner.getUserId(), partner == null ? null : partner.getOrganisationName(),
                enrolled, waitlisted, activity.getLevelFeatures());
    }

    public EnrollmentResponse toEnrollmentResponse(Enrollment enrollment) {
        return new EnrollmentResponse(enrollment.getEnrollmentId(), enrollment.getActivity().getActivityId(),
                enrollment.getActivity().getTitle(), enrollment.getYouth().getUserId(),
                enrollment.getYouth().getName(), enrollment.getYouth().getEmail(), enrollment.getStatus(),
                enrollment.getEnrollmentDate(), enrollment.getCompletedAt());
    }

    private void promoteWaitlist(LearningActivity activity) {
        enrollmentRepository.findFirstByActivityActivityIdAndStatusOrderByEnrollmentDateAsc(activity.getActivityId(),
                EnrollmentStatus.WAITLISTED).ifPresent(next -> {
                    long enrolled = enrollmentRepository.countByActivityActivityIdAndStatus(activity.getActivityId(),
                            EnrollmentStatus.ENROLLED);
                    if (activity.hasCapacity((int) enrolled)) {
                        next.confirmEnrollment();
                        enrollmentRepository.save(next);
                        notificationService.notify(next.getYouth(),
                                "A place opened up. You are now enrolled in " + activity.getTitle() + ".",
                                "/exchanges/" + activity.getActivityId(), NotificationType.IN_APP);
                    }
                });
    }

    private YouthParticipant currentParticipant() {
        User user = userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new ForbiddenException("FORBIDDEN", "Participant account required."));
        if (!(user instanceof YouthParticipant participant)) {
            throw new ForbiddenException("PARTICIPANT_ONLY", "Only youth participants can enroll.");
        }
        return participant;
    }

    private PartnerOrganisation findPartner(Long partnerId) {
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("PARTNER_NOT_FOUND", "Partner organisation not found."));
    }

    private void validateActivity(LearningActivity activity) {
        if (activity.getEndDate() != null && activity.getEndDate().isBefore(activity.getStartDate())) {
            throw new BadRequestException("INVALID_DATES", "End date cannot be before the start date.");
        }
        if (activity.getMinTeamSize() > activity.getMaxTeamSize()) {
            throw new BadRequestException("INVALID_TEAM_SIZES", "Minimum team size cannot exceed maximum team size.");
        }
        if (activity.getPassMark() < 0 || activity.getPassMark() > 100) {
            throw new BadRequestException("INVALID_PASS_MARK", "Pass mark must be between 0 and 100.");
        }
        if (activity.getMinAttendancePercent() < 0 || activity.getMinAttendancePercent() > 100) {
            throw new BadRequestException("INVALID_ATTENDANCE", "Attendance threshold must be between 0 and 100.");
        }
    }

    private User actorUser(AuthenticatedUser actor) {
        return userRepository.findById(actor.userId()).orElse(null);
    }

    private User currentActorUser() {
        return actorUser(securityUtils.currentUser());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
