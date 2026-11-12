package com.yep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.yep.domain.enums.ChallengeArea;
import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.ReviewDecision;
import com.yep.domain.enums.TeamStatus;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.ActivityResponse;
import com.yep.dto.LearningDtos.CreateActivityRequest;
import com.yep.dto.LearningDtos.EnrollmentDecisionRequest;
import com.yep.dto.ProjectDtos.AddTeamMemberRequest;
import com.yep.dto.ProjectDtos.MentorAssignmentRequest;
import com.yep.dto.ProjectDtos.MilestoneRequest;
import com.yep.dto.ProjectDtos.MilestoneResponse;
import com.yep.dto.ProjectDtos.MilestoneStatusRequest;
import com.yep.dto.ProjectDtos.ProjectRequest;
import com.yep.dto.ProjectDtos.ProjectResponse;
import com.yep.dto.ProjectDtos.ReviewRequest;
import com.yep.dto.ProjectDtos.ReviewResponse;
import com.yep.dto.ProjectDtos.TeamRequest;
import com.yep.dto.ProjectDtos.TeamResponse;
import com.yep.exception.BusinessRuleException;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.service.ActivityService;
import com.yep.service.ProjectService;
import com.yep.service.TeamService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectFlowTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ActivityService activityService;
    @Autowired
    private TeamService teamService;
    @Autowired
    private ProjectService projectService;

    private Facilitator facilitator;
    private Mentor mentor;
    private Mentor otherMentor;
    private YouthParticipant first;
    private YouthParticipant second;
    private YouthParticipant third;
    private ActivityResponse activity;

    @BeforeEach
    void setUp() {
        facilitator = new Facilitator();
        facilitator.setName("Level Two Facilitator");
        facilitator.setEmail("l2.facilitator@yep.test");
        facilitator.setPassword(passwordEncoder.encode("Password1"));
        facilitator.setStatus(AccountStatus.ACTIVE);
        facilitator = userRepository.save(facilitator);

        mentor = mentor("mentor.one@yep.test", "Mentor One");
        otherMentor = mentor("mentor.two@yep.test", "Mentor Two");
        first = participant("l2.first@yep.test", "First Participant");
        second = participant("l2.second@yep.test", "Second Participant");
        third = participant("l2.third@yep.test", "Third Participant");

        loginAs(facilitator);
        activity = activityService.create(new CreateActivityRequest(ActivityLevel.TWO, "Impact Lab",
                "Build a community impact project", LocalDate.now(), LocalDate.now().plusDays(30), "Digital",
                10, null, null, 50, 75, 3, 5));
        activityService.changeStatus(activity.activityId(), ActivityStatus.PUBLISHED);

        // Level Two uses the apply/select flow: participants apply, the facilitator selects them.
        List<Long> applications = new java.util.ArrayList<>();
        for (YouthParticipant youth : List.of(first, second, third)) {
            youth.setLevelTwoEligible(true);
            userRepository.save(youth);
            loginAs(youth);
            applications.add(activityService.apply(activity.activityId()).enrollmentId());
        }
        loginAs(facilitator);
        for (Long enrollmentId : applications) {
            activityService.decide(enrollmentId, new EnrollmentDecisionRequest("SELECT"));
        }
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void teamBuildsProjectMentorApprovesAndProjectBecomesShowcaseReady() {
        loginAs(first);
        TeamResponse team = teamService.create(new TeamRequest(activity.activityId(), "Team Impact", null));
        assertThat(team.status()).isEqualTo(TeamStatus.OPEN);
        assertThat(team.memberCount()).isEqualTo(1);

        loginAs(facilitator);
        team = teamService.addMember(team.teamId(), new AddTeamMemberRequest(second.getUserId()));
        team = teamService.addMember(team.teamId(), new AddTeamMemberRequest(third.getUserId()));
        assertThat(team.memberCount()).isEqualTo(3);
        assertThat(team.status()).isEqualTo(TeamStatus.OPEN);

        team = teamService.assignMentor(team.teamId(), new MentorAssignmentRequest(mentor.getUserId()));
        assertThat(team.mentorId()).isEqualTo(mentor.getUserId());
        team = teamService.lock(team.teamId());
        assertThat(team.status()).isEqualTo(TeamStatus.LOCKED);

        loginAs(first);
        ProjectResponse project = projectService.create(new ProjectRequest(team.teamId(), "Clean Water Map",
                "Map water points in our community", ChallengeArea.CLIMATE_CHANGE, false, null));
        assertThat(project.status()).isEqualTo(ProjectStatus.DRAFT);

        MilestoneResponse firstMilestone = projectService.addMilestone(project.projectId(),
                new MilestoneRequest("Community survey", LocalDate.now().plusDays(5)));
        projectService.addMilestone(project.projectId(),
                new MilestoneRequest("Publish map", LocalDate.now().plusDays(15)));
        projectService.updateMilestone(firstMilestone.milestoneId(),
                new MilestoneStatusRequest(MilestoneStatus.DONE));

        project = projectService.submit(project.projectId());
        assertThat(project.status()).isEqualTo(ProjectStatus.SUBMITTED);
        assertThat(project.progressPercent()).isEqualTo(50);
        Long projectId = project.projectId();

        loginAs(otherMentor);
        assertThatThrownBy(() -> projectService.review(projectId,
                new ReviewRequest(ReviewDecision.APPROVED, "Looks good")))
                .isInstanceOf(com.yep.exception.ForbiddenException.class);

        loginAs(mentor);
        ReviewResponse review = projectService.review(projectId,
                new ReviewRequest(ReviewDecision.APPROVED, "Strong community evidence and clear milestones."));
        assertThat(review.decision()).isEqualTo(ReviewDecision.APPROVED);
        ProjectResponse approved = projectService.get(project.projectId());
        assertThat(approved.status()).isEqualTo(ProjectStatus.APPROVED);
        assertThat(approved.reviews()).hasSize(1);

        loginAs(first);
        ProjectResponse visible = projectService.setSupportVisibility(project.projectId(), true);
        assertThat(visible.openForSupport()).isTrue();
        assertThat(projectService.showcase()).extracting(ProjectResponse::projectId)
                .contains(project.projectId());

        loginAs(facilitator);
        ProjectResponse completed = projectService.complete(project.projectId());
        assertThat(completed.status()).isEqualTo(ProjectStatus.COMPLETED);
    }

    @Test
    void aiDeclarationIsRequiredAndChangesCanBeRequested() {
        loginAs(first);
        TeamResponse team = teamService.create(new TeamRequest(activity.activityId(), "AI Team", null));
        loginAs(facilitator);
        teamService.addMember(team.teamId(), new AddTeamMemberRequest(second.getUserId()));
        teamService.addMember(team.teamId(), new AddTeamMemberRequest(third.getUserId()));
        teamService.assignMentor(team.teamId(), new MentorAssignmentRequest(mentor.getUserId()));

        loginAs(first);
        assertThatThrownBy(() -> projectService.create(new ProjectRequest(team.teamId(), "AI Project",
                "Use AI to analyse survey data", ChallengeArea.EDUCATION, true, " ")))
                .isInstanceOf(BusinessRuleException.class);

        ProjectResponse project = projectService.create(new ProjectRequest(team.teamId(), "AI Project",
                "Use AI to analyse survey data", ChallengeArea.EDUCATION, true,
                "AI was used only to summarise anonymised survey responses."));
        projectService.addMilestone(project.projectId(),
                new MilestoneRequest("Collect data", LocalDate.now().plusDays(3)));
        projectService.addMilestone(project.projectId(),
                new MilestoneRequest("Review findings", LocalDate.now().plusDays(9)));
        projectService.submit(project.projectId());

        loginAs(mentor);
        projectService.review(project.projectId(),
                new ReviewRequest(ReviewDecision.CHANGES_REQUESTED, "Please add a data privacy note."));
        loginAs(first);
        ProjectResponse changed = projectService.get(project.projectId());
        assertThat(changed.status()).isEqualTo(ProjectStatus.CHANGES_REQUESTED);
        assertThat(changed.aiUsageDeclaration()).contains("anonymised");
    }

    private Mentor mentor(String email, String name) {
        Mentor value = new Mentor();
        value.setName(name);
        value.setEmail(email);
        value.setPassword(passwordEncoder.encode("Password1"));
        value.setStatus(AccountStatus.ACTIVE);
        value.setExpertise("Community projects");
        return userRepository.save(value);
    }

    private YouthParticipant participant(String email, String name) {
        YouthParticipant youth = new YouthParticipant();
        youth.setName(name);
        youth.setEmail(email);
        youth.setPassword(passwordEncoder.encode("Password1"));
        youth.setStatus(AccountStatus.ACTIVE);
        youth.setAge(24);
        youth.setCountry("Kenya");
        youth.setDigitalSkills("Intermediate");
        youth.setLevelTwoEligible(true);
        return userRepository.save(youth);
    }

    private void loginAs(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getUserId(), user.getEmail(), user.getName(),
                user.getRole());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.authorities()));
    }
}
