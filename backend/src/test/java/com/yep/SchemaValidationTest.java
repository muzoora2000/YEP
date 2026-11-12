package com.yep;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LevelOneActivity;
import com.yep.domain.activity.LevelTwoActivity;
import com.yep.domain.activity.LearningActivity;
import com.yep.domain.enums.ActivityLevel;
import com.yep.domain.enums.ChallengeArea;
import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.enums.TaskType;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.learning.AssessmentTask;
import com.yep.domain.learning.DiscussionPost;
import com.yep.domain.learning.DiscussionThread;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.project.Milestone;
import com.yep.domain.project.Team;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.repository.AssessmentTaskRepository;
import com.yep.repository.DiscussionPostRepository;
import com.yep.repository.DiscussionThreadRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.LearningActivityRepository;
import com.yep.repository.MilestoneRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;

/**
 * Boots the application with the real Flyway migrations and ddl-auto=validate.
 * Any mismatch between SQL and the JPA inheritance model fails this test.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaValidationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LearningActivityRepository activityRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private ImpactProjectRepository projectRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private DiscussionThreadRepository threadRepository;

    @Autowired
    private DiscussionPostRepository postRepository;

    @Autowired
    private AssessmentTaskRepository taskRepository;

    @Test
    void flywaySchemaMatchesJpaEntities() {
        assertThat(userRepository).isNotNull();
        assertThat(activityRepository).isNotNull();
    }

    @Test
    void joinedInheritanceStoresEveryProfileInItsOwnTable() {
        YouthParticipant participant = participant("schema-participant");
        Facilitator facilitator = facilitator("schema-facilitator");
        userRepository.saveAndFlush(participant);
        userRepository.saveAndFlush(facilitator);

        User reloaded = userRepository.findByEmailIgnoreCase(participant.getEmail()).orElseThrow();
        assertThat(reloaded).isInstanceOf(YouthParticipant.class);
        assertThat(reloaded.getRole()).isEqualTo(com.yep.domain.enums.Role.PARTICIPANT);
        assertThat(((YouthParticipant) reloaded).getAge()).isEqualTo(24);
    }

    @Test
    void singleTableActivityInheritanceIsPolymorphic() {
        Facilitator facilitator = facilitator("activity-owner");
        userRepository.saveAndFlush(facilitator);

        LevelOneActivity one = new LevelOneActivity();
        one.setFacilitator(facilitator);
        one.setTitle("AI literacy");
        one.setStartDate(LocalDate.now().plusDays(2));
        activityRepository.save(one);

        LevelTwoActivity two = new LevelTwoActivity();
        two.setFacilitator(facilitator);
        two.setTitle("Digital inclusion projects");
        two.setStartDate(LocalDate.now().plusDays(3));
        activityRepository.save(two);

        List<LearningActivity> all = activityRepository.findAll();
        assertThat(all).anyMatch(LevelOneActivity.class::isInstance);
        assertThat(all).anyMatch(LevelTwoActivity.class::isInstance);
        assertThat(all).allSatisfy(activity -> {
            assertThat(activity.getLevelFeatures()).isNotEmpty();
            if (activity.getLevel() == ActivityLevel.ONE) {
                assertThat(activity.supportsTeamFormation()).isFalse();
            } else {
                assertThat(activity.supportsTeamFormation()).isTrue();
            }
        });
    }

    @Test
    void enrollmentStatusOnlyChangesThroughDomainMethods() {
        YouthParticipant participant = participant("enrollment-youth");
        Facilitator facilitator = facilitator("enrollment-facilitator");
        userRepository.saveAndFlush(participant);
        userRepository.saveAndFlush(facilitator);

        LevelOneActivity activity = new LevelOneActivity();
        activity.setFacilitator(facilitator);
        activity.setTitle("Media literacy");
        activity.setStartDate(LocalDate.now().plusDays(1));
        activityRepository.saveAndFlush(activity);

        Enrollment enrollment = participant.enrollActivity(activity);
        enrollmentRepository.saveAndFlush(enrollment);

        assertThat(enrollment.getStatus()).isEqualTo(com.yep.domain.enums.EnrollmentStatus.ENROLLED);
        enrollment.cancelEnrollment();
        assertThat(enrollment.getStatus()).isEqualTo(com.yep.domain.enums.EnrollmentStatus.CANCELLED);
    }

    @Test
    void teamProjectAndCompositionRelationshipsPersist() {
        Facilitator facilitator = facilitator("project-facilitator");
        userRepository.saveAndFlush(facilitator);
        YouthParticipant leader = participant("project-leader");
        userRepository.saveAndFlush(leader);

        LevelTwoActivity activity = new LevelTwoActivity();
        activity.setFacilitator(facilitator);
        activity.setTitle("Climate teams");
        activity.setStartDate(LocalDate.now());
        activityRepository.saveAndFlush(activity);

        Team team = new Team(activity, "Green Squad", leader);
        team.addMember(leader, TeamMemberRole.LEADER);
        team = teamRepository.saveAndFlush(team);

        ImpactProject project = new ImpactProject(team, "Solar dryers");
        project.setChallengeArea(ChallengeArea.CLIMATE_CHANGE);
        project.setDescription("A community solar drying project");
        project.addMilestone(new Milestone(project, "Research", LocalDate.now().plusDays(10)));
        project.addMilestone(new Milestone(project, "Prototype", LocalDate.now().plusDays(20)));
        project = projectRepository.saveAndFlush(project);

        assertThat(team.memberCount()).isEqualTo(1);
        assertThat(project.progressPercent()).isZero();
        Milestone first = project.getMilestones().get(0);
        first.updateMilestone(MilestoneStatus.DONE);
        milestoneRepository.saveAndFlush(first);
        assertThat(project.progressPercent()).isEqualTo(50);
    }

    @Test
    void discussionAndAssessmentJsonColumnsPersist() {
        Facilitator facilitator = facilitator("json-facilitator");
        userRepository.saveAndFlush(facilitator);

        LevelOneActivity activity = new LevelOneActivity();
        activity.setFacilitator(facilitator);
        activity.setTitle("Critical thinking");
        activity.setStartDate(LocalDate.now());
        activityRepository.saveAndFlush(activity);

        DiscussionThread thread = new DiscussionThread(activity, "General");
        thread = threadRepository.saveAndFlush(thread);
        DiscussionPost post = new DiscussionPost(thread, facilitator, "Welcome to the exchange");
        postRepository.saveAndFlush(post);

        AssessmentTask task = new AssessmentTask(activity, "Quiz one", TaskType.QUIZ);
        task.setQuestions(List.of(java.util.Map.of(
                "text", "What is misinformation?",
                "options", List.of("A", "B"),
                "correctIndex", 0)));
        taskRepository.saveAndFlush(task);

        assertThat(threadRepository.findByActivityActivityId(activity.getActivityId())).isNotEmpty();
        assertThat(postRepository.findByThreadThreadIdOrderByPostedAtAsc(thread.getThreadId())).hasSize(1);
        assertThat(taskRepository.findByActivityActivityIdAndPublishedTrueOrderByDueDateAsc(activity.getActivityId()))
                .hasSize(1);
    }

    private YouthParticipant participant(String prefix) {
        YouthParticipant participant = new YouthParticipant();
        participant.setName("Schema Participant");
        participant.setEmail(prefix + "-" + UUID.randomUUID() + "@yep.test");
        participant.setPassword("$2a$10$01234567890123456789012345678901234567890123456789012");
        participant.setAge(24);
        participant.setCountry("Uganda");
        participant.setDigitalSkills("INTERMEDIATE");
        participant.register();
        return participant;
    }

    private Facilitator facilitator(String prefix) {
        Facilitator facilitator = new Facilitator();
        facilitator.setName("Schema Facilitator");
        facilitator.setEmail(prefix + "-" + UUID.randomUUID() + "@yep.test");
        facilitator.setPassword("$2a$10$01234567890123456789012345678901234567890123456789012");
        facilitator.setSpecialisation("Digital literacy");
        facilitator.register();
        return facilitator;
    }
}
