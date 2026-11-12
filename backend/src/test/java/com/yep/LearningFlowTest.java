package com.yep;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.ActivityLevel;
import com.yep.domain.enums.ActivityStatus;
import com.yep.domain.enums.AttendanceStatus;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.SessionStatus;
import com.yep.domain.enums.TaskType;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.ActivityCompletionSummary;
import com.yep.dto.LearningDtos.ActivityResponse;
import com.yep.dto.LearningDtos.AssessmentResponse;
import com.yep.dto.LearningDtos.AttendanceResponse;
import com.yep.dto.LearningDtos.CertificateResponse;
import com.yep.dto.LearningDtos.CreateActivityRequest;
import com.yep.dto.LearningDtos.EnrollmentResponse;
import com.yep.dto.LearningDtos.GradeRequest;
import com.yep.dto.LearningDtos.MaterialRequest;
import com.yep.dto.LearningDtos.MaterialResponse;
import com.yep.dto.LearningDtos.PostRequest;
import com.yep.dto.LearningDtos.PostResponse;
import com.yep.dto.LearningDtos.ProgressResponse;
import com.yep.dto.LearningDtos.SessionRequest;
import com.yep.dto.LearningDtos.SessionResponse;
import com.yep.dto.LearningDtos.SubmissionRequest;
import com.yep.dto.LearningDtos.TaskRequest;
import com.yep.dto.LearningDtos.TaskResponse;
import com.yep.dto.LearningDtos.ThreadRequest;
import com.yep.domain.enums.MaterialType;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.service.ActivityService;
import com.yep.service.ActivityCompletionService;
import com.yep.service.AssessmentService;
import com.yep.service.CertificateService;
import com.yep.service.DiscussionService;
import com.yep.service.MaterialService;
import com.yep.service.ProgressService;
import com.yep.service.SessionService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LearningFlowTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private DiscussionService discussionService;
    @Autowired
    private AssessmentService assessmentService;
    @Autowired
    private ProgressService progressService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private ActivityCompletionService completionService;

    private Facilitator facilitator;
    private YouthParticipant participant;

    @BeforeEach
    void setUp() {
        facilitator = new Facilitator();
        facilitator.setName("Facilitator One");
        facilitator.setEmail("facilitator.one@yep.test");
        facilitator.setPassword(passwordEncoder.encode("Password1"));
        facilitator.setStatus(AccountStatus.ACTIVE);
        facilitator.setSpecialisation("Digital literacy");
        facilitator = userRepository.save(facilitator);

        participant = participant("participant.one@yep.test", "Amina One", 22);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completeLevelOneJourneyIssuesCertificateAndUnlocksLevelTwo() {
        loginAs(facilitator);
        ActivityResponse activity = createAndPublish(ActivityLevel.ONE, "Digital Foundations", 1, 100);
        SessionResponse session = sessionService.create(activity.activityId(),
                new SessionRequest("Live welcome", OffsetDateTime.now(), 60, "https://meet.example.com/yep"));
        materialService.create(activity.activityId(),
                new MaterialRequest("Welcome pack", MaterialType.PDF, "https://example.com/welcome.pdf", "Start here", true));
        Long threadId = discussionService.createThread(activity.activityId(),
                new ThreadRequest("Introductions", null)).threadId();
        TaskResponse task = assessmentService.createTask(activity.activityId(),
                new TaskRequest("Digital reflection", TaskType.ASSIGNMENT, "Write a short reflection", null, 100,
                        OffsetDateTime.now().plusDays(2), true));

        loginAs(participant);
        EnrollmentResponse enrollment = activityService.enroll(activity.activityId());
        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.ENROLLED);
        AttendanceResponse attendance = sessionService.checkIn(session.sessionId());
        assertThat(attendance.status()).isEqualTo(AttendanceStatus.PRESENT);
        // Attendance % is measured over COMPLETED sessions, so close the session first.
        loginAs(facilitator);
        sessionService.changeStatus(session.sessionId(), SessionStatus.COMPLETED);
        loginAs(participant);
        List<MaterialResponse> materials = materialService.list(activity.activityId());
        assertThat(materials).hasSize(1);
        PostResponse post = discussionService.addPost(threadId, new PostRequest("Hello from Amina"));
        assertThat(post.authorName()).isEqualTo("Amina One");
        AssessmentResponse submission = assessmentService.submit(task.taskId(),
                new SubmissionRequest(List.of(), "I learned about responsible AI."));
        assertThat(submission.status().name()).isEqualTo("SUBMITTED");

        loginAs(facilitator);
        AssessmentResponse graded = assessmentService.grade(submission.assessmentId(),
                new GradeRequest(80, "Strong reflection"));
        assertThat(graded.percentage()).isEqualTo(80.0);
        ProgressResponse progress = progressService.progressFor(activity.activityId(), participant.getUserId());
        assertThat(progress.attendancePercent()).isEqualTo(100.0);
        assertThat(progress.assessmentAverage()).isEqualTo(80.0);
        assertThat(progress.passedAssessments()).isTrue();
        assertThat(progress.passedAttendance()).isTrue();

        CertificateResponse certificate = certificateService.issue(activity.activityId(), participant.getUserId());
        assertThat(certificate.certificateNumber()).startsWith("YEP-");
        assertThat(certificateService.verify(certificate.certificateNumber()).youthName()).isEqualTo("Amina One");
        assertThat(certificateService.pdf(certificate.certificateNumber())).startsWith("%PDF".getBytes());

        YouthParticipant refreshed = (YouthParticipant) userRepository.findById(participant.getUserId()).orElseThrow();
        assertThat(refreshed.isLevelTwoEligible()).isTrue();

        ActivityResponse levelTwo = createAndPublish(ActivityLevel.TWO, "Impact Lab", 5, 100);
        loginAs(refreshed);
        EnrollmentResponse levelTwoEnrollment = activityService.apply(levelTwo.activityId());
        assertThat(levelTwoEnrollment.status()).isEqualTo(EnrollmentStatus.APPLIED);
    }

    @Test
    void completionWorkflowIssuesCertificatesForEligibleRoster() {
        loginAs(facilitator);
        ActivityResponse activity = createAndPublish(ActivityLevel.ONE, "Completion Cohort", 10, 100);
        SessionResponse session = sessionService.create(activity.activityId(),
                new SessionRequest("Only session", OffsetDateTime.now(), 45, "https://meet.example.com/done"));
        TaskResponse task = assessmentService.createTask(activity.activityId(),
                new TaskRequest("Final task", TaskType.ASSIGNMENT, "Reflect", null, 100,
                        OffsetDateTime.now().plusDays(1), true));

        loginAs(participant);
        activityService.enroll(activity.activityId());
        sessionService.checkIn(session.sessionId());
        AssessmentResponse submission = assessmentService.submit(task.taskId(),
                new SubmissionRequest(List.of(), "Reflection"));

        loginAs(facilitator);
        sessionService.changeStatus(session.sessionId(), SessionStatus.COMPLETED);
        assessmentService.grade(submission.assessmentId(), new GradeRequest(90, "Excellent"));

        ActivityCompletionSummary summary = completionService.complete(activity.activityId());
        assertThat(summary.certificatesIssued()).isEqualTo(1);
        assertThat(summary.participantsCompleted()).isEqualTo(1);
        assertThat(activityService.manageGet(activity.activityId()).status()).isEqualTo(ActivityStatus.COMPLETED);

        loginAs(participant);
        assertThat(certificateService.myCertificates()).hasSize(1);
    }

    @Test
    void capacityWaitlistPromotesNextParticipantWhenSomeoneCancels() {
        loginAs(facilitator);
        ActivityResponse activity = createAndPublish(ActivityLevel.ONE, "One Seat Only", 1, 75);
        YouthParticipant second = participant("participant.two@yep.test", "Amina Two", 25);

        loginAs(participant);
        EnrollmentResponse first = activityService.enroll(activity.activityId());
        loginAs(second);
        EnrollmentResponse waitlisted = activityService.enroll(activity.activityId());
        assertThat(waitlisted.status()).isEqualTo(EnrollmentStatus.WAITLISTED);

        loginAs(participant);
        activityService.cancelEnrollment(first.enrollmentId());
        var promoted = enrollmentRepository.findByYouthUserIdAndActivityActivityId(second.getUserId(),
                activity.activityId()).orElseThrow();
        assertThat(promoted.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
    }

    private ActivityResponse createAndPublish(ActivityLevel level, String title, int capacity, int attendance) {
        loginAs(facilitator);
        ActivityResponse activity = activityService.create(new CreateActivityRequest(level, title, "Description",
                LocalDate.now(), LocalDate.now().plusDays(14), "Digital", capacity, null, null, 50, attendance, 3, 5));
        activityService.changeStatus(activity.activityId(), ActivityStatus.PUBLISHED);
        return activity;
    }

    private YouthParticipant participant(String email, String name, int age) {
        YouthParticipant youth = new YouthParticipant();
        youth.setName(name);
        youth.setEmail(email);
        youth.setPassword(passwordEncoder.encode("Password1"));
        youth.setStatus(AccountStatus.ACTIVE);
        youth.setAge(age);
        youth.setCountry("Kenya");
        youth.setDigitalSkills("Beginner");
        return userRepository.save(youth);
    }

    private void loginAs(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getUserId(), user.getEmail(), user.getName(),
                user.getRole());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }
}
