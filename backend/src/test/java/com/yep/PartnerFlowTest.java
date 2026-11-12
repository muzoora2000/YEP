package com.yep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
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
import com.yep.domain.enums.OpportunityType;
import com.yep.domain.enums.ReviewDecision;
import com.yep.domain.enums.SupportStatus;
import com.yep.domain.enums.SupportType;
import com.yep.domain.user.Administrator;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.PartnerOrganisation;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.LearningDtos.ActivityResponse;
import com.yep.dto.LearningDtos.CreateActivityRequest;
import com.yep.dto.LearningDtos.EnrollmentDecisionRequest;
import com.yep.dto.PartnerDtos.DashboardResponse;
import com.yep.dto.PartnerDtos.InterestRequest;
import com.yep.dto.PartnerDtos.InterestResponse;
import com.yep.dto.PartnerDtos.OpportunityRequest;
import com.yep.dto.PartnerDtos.OpportunityResponse;
import com.yep.dto.PartnerDtos.SupportRequest;
import com.yep.dto.PartnerDtos.SupportResponse;
import com.yep.dto.ProjectDtos.AddTeamMemberRequest;
import com.yep.dto.ProjectDtos.MentorAssignmentRequest;
import com.yep.dto.ProjectDtos.MilestoneRequest;
import com.yep.dto.ProjectDtos.ProjectRequest;
import com.yep.dto.ProjectDtos.ProjectResponse;
import com.yep.dto.ProjectDtos.ReviewRequest;
import com.yep.dto.ProjectDtos.TeamRequest;
import com.yep.dto.ProjectDtos.TeamResponse;
import com.yep.exception.ForbiddenException;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.service.ActivityService;
import com.yep.service.AdminPlatformService;
import com.yep.service.NotificationService;
import com.yep.service.PartnerService;
import com.yep.service.ProjectService;
import com.yep.service.TeamService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PartnerFlowTest {

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
    @Autowired
    private PartnerService partnerService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private AdminPlatformService adminPlatformService;

    private Administrator admin;
    private Facilitator facilitator;
    private Mentor mentor;
    private PartnerOrganisation partner;
    private PartnerOrganisation unverifiedPartner;
    private YouthParticipant first;
    private YouthParticipant second;
    private YouthParticipant third;
    private ActivityResponse activity;

    @BeforeEach
    void setUp() {
        admin = new Administrator();
        admin.setName("Platform Admin");
        admin.setEmail("admin@yep.test");
        admin.setPassword(passwordEncoder.encode("Password1"));
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setDepartment("Programmes");
        admin = userRepository.save(admin);

        facilitator = new Facilitator();
        facilitator.setName("Facilitator");
        facilitator.setEmail("partner.facilitator@yep.test");
        facilitator.setPassword(passwordEncoder.encode("Password1"));
        facilitator.setStatus(AccountStatus.ACTIVE);
        facilitator = userRepository.save(facilitator);

        mentor = new Mentor();
        mentor.setName("Mentor");
        mentor.setEmail("partner.mentor@yep.test");
        mentor.setPassword(passwordEncoder.encode("Password1"));
        mentor.setStatus(AccountStatus.ACTIVE);
        mentor = userRepository.save(mentor);

        partner = partner("partner@yep.test", "Partner User", "Impact Foundation", true);
        unverifiedPartner = partner("unverified@yep.test", "Unverified User", "New Partner", false);
        first = participant("partner.first@yep.test", "First Youth");
        second = participant("partner.second@yep.test", "Second Youth");
        third = participant("partner.third@yep.test", "Third Youth");

        loginAs(facilitator);
        activity = activityService.create(new CreateActivityRequest(ActivityLevel.TWO, "Partner Impact Lab",
                "Project showcase", LocalDate.now(), LocalDate.now().plusDays(30), "Digital", 10, null, null,
                50, 75, 3, 5));
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
    void partnerPublishesOpportunityAndReceivesParticipantInterestNotification() {
        loginAs(partner);
        OpportunityResponse opportunity = partnerService.createOpportunity(new OpportunityRequest(
                "Junior Data Analyst", OpportunityType.JOB, "Support a local health data team.",
                "Nairobi / Hybrid", LocalDate.now().plusDays(20)));
        assertThat(opportunity.interestCount()).isZero();

        assertThat(partnerService.publicOpportunities(org.springframework.data.domain.PageRequest.of(0, 10))
                .getContent()).extracting(OpportunityResponse::opportunityId)
                .contains(opportunity.opportunityId());

        loginAs(first);
        InterestResponse interest = partnerService.expressInterest(opportunity.opportunityId(),
                new InterestRequest("I have built dashboards in Excel and Python."));
        assertThat(interest.youthName()).isEqualTo("First Youth");

        loginAs(partner);
        assertThat(partnerService.opportunityInterests(opportunity.opportunityId())).hasSize(1);
        assertThat(notificationService.myNotifications()).anyMatch(
                notification -> notification.message().contains("First Youth"));
        assertThat(notificationService.unreadCount()).isPositive();

        notificationService.markAllRead();
        assertThat(notificationService.unreadCount()).isZero();

        loginAs(first);
        assertThat(partnerService.myInterests()).hasSize(1);
    }

    @Test
    void partnerSupportsApprovedProjectAndAdminReportsPlatformTotals() {
        ProjectResponse project = approvedShowcaseProject();

        loginAs(partner);
        SupportResponse support = partnerService.offerSupport(project.projectId(), new SupportRequest(
                SupportType.MENTORSHIP, "We can provide two mentors for six weeks."));
        assertThat(support.status()).isEqualTo(SupportStatus.OFFERED);

        loginAs(first);
        assertThat(partnerService.projectSupports(project.projectId())).hasSize(1);
        SupportResponse accepted = partnerService.respondToSupport(support.supportId(), true);
        assertThat(accepted.status()).isEqualTo(SupportStatus.ACCEPTED);
        assertThat(notificationService.myNotifications()).anyMatch(
                notification -> notification.message().contains("Impact Foundation"));

        loginAs(admin);
        DashboardResponse dashboard = adminPlatformService.dashboard();
        assertThat(dashboard.totalUsers()).isGreaterThan(5);
        assertThat(dashboard.participants()).isEqualTo(3);
        assertThat(dashboard.partners()).isEqualTo(2);
        assertThat(dashboard.projects()).isPositive();
        assertThat(dashboard.verifiedPartners()).isEqualTo(1);
        assertThat(adminPlatformService.auditLogs(org.springframework.data.domain.PageRequest.of(0, 5))
                .getTotalElements()).isPositive();

        var updated = adminPlatformService.updateSetting("platform_name", "YEP Test Platform");
        assertThat(updated.value()).isEqualTo("YEP Test Platform");
        assertThat(adminPlatformService.publicSettings().platformName()).isEqualTo("YEP Test Platform");
        assertThat(adminPlatformService.publicSettings().countries()).contains("Kenya");
    }

    @Test
    void unverifiedPartnerCannotPublishOpportunity() {
        loginAs(unverifiedPartner);
        assertThatThrownBy(() -> partnerService.createOpportunity(new OpportunityRequest(
                "Blocked Opportunity", OpportunityType.TRAINING, "Not allowed yet", "Remote", null)))
                .isInstanceOf(ForbiddenException.class);
    }

    private ProjectResponse approvedShowcaseProject() {
        loginAs(first);
        TeamResponse team = teamService.create(new TeamRequest(activity.activityId(), "Partner Team", null));
        loginAs(facilitator);
        teamService.addMember(team.teamId(), new AddTeamMemberRequest(second.getUserId()));
        teamService.addMember(team.teamId(), new AddTeamMemberRequest(third.getUserId()));
        teamService.assignMentor(team.teamId(), new MentorAssignmentRequest(mentor.getUserId()));

        loginAs(first);
        ProjectResponse project = projectService.create(new ProjectRequest(team.teamId(), "Health Data Map",
                "A community health data project", ChallengeArea.HEALTH, false, null));
        projectService.addMilestone(project.projectId(),
                new MilestoneRequest("Collect data", LocalDate.now().plusDays(3)));
        projectService.addMilestone(project.projectId(),
                new MilestoneRequest("Publish findings", LocalDate.now().plusDays(10)));
        projectService.submit(project.projectId());

        loginAs(mentor);
        projectService.review(project.projectId(),
                new ReviewRequest(ReviewDecision.APPROVED, "Clear impact and strong data ethics."));
        loginAs(first);
        return projectService.setSupportVisibility(project.projectId(), true);
    }

    private PartnerOrganisation partner(String email, String name, String organisation, boolean verified) {
        PartnerOrganisation value = new PartnerOrganisation();
        value.setName(name);
        value.setEmail(email);
        value.setPassword(passwordEncoder.encode("Password1"));
        value.setStatus(AccountStatus.ACTIVE);
        value.setOrganisationName(organisation);
        value.setCountry("Kenya");
        value.setVerified(verified);
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
