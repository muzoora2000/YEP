package com.yep.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.yep.domain.assessment.Assessment;
import com.yep.domain.certificate.Certificate;
import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.CompletionStatus;
import com.yep.domain.enums.ExchangeLevel;
import com.yep.domain.enums.ExchangeStatus;
import com.yep.domain.enums.LevelOneTopic;
import com.yep.domain.enums.LevelTwoFocusArea;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.enums.SkillLevel;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.exchange.Enrollment;
import com.yep.domain.exchange.LevelOneExchange;
import com.yep.domain.exchange.LevelTwoExchange;
import com.yep.domain.message.Announcement;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.team.Team;
import com.yep.domain.user.Admin;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;
import com.yep.repository.AssessmentRepository;
import com.yep.repository.CertificateRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.LevelOneExchangeRepository;
import com.yep.repository.LevelTwoExchangeRepository;
import com.yep.repository.MessageRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;

/**
 * Demo data for the {@code dev} / {@code h2} profiles so the API can be explored
 * immediately after start-up (never runs in {@code prod}).
 *
 * <p>All demo passwords are {@code Password123!}.</p>
 */
@Configuration
@Profile({ "dev", "h2" })
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final LevelOneExchangeRepository levelOneRepository;
    private final LevelTwoExchangeRepository levelTwoRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ImpactProjectRepository projectRepository;
    private final AssessmentRepository assessmentRepository;
    private final CertificateRepository certificateRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            LevelOneExchangeRepository levelOneRepository,
            LevelTwoExchangeRepository levelTwoRepository,
            EnrollmentRepository enrollmentRepository,
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            ImpactProjectRepository projectRepository,
            AssessmentRepository assessmentRepository,
            CertificateRepository certificateRepository,
            MessageRepository messageRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.levelOneRepository = levelOneRepository;
        this.levelTwoRepository = levelTwoRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.projectRepository = projectRepository;
        this.assessmentRepository = assessmentRepository;
        this.certificateRepository = certificateRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Profile({ "dev", "h2" })
    public CommandLineRunner seedDemoData() {
        return args -> seed();
    }

    /** Each repository call runs in its own transaction, so no outer transaction is needed. */
    protected void seed() {
        if (userRepository.count() > 0) {
            log.info("Database already contains data - skipping demo seed");
            return;
        }

        Admin admin = (Admin) user(new Admin(), Role.ADMIN, "YEP Admin", "admin@yep.africa");
        admin.setPermissions("users:read,users:write,exchanges:write,teams:write,certificates:write,*");

        Facilitator amara = (Facilitator) user(new Facilitator(), Role.FACILITATOR, "Amara Okello",
                "amara@yep.africa");
        amara.setExpertiseArea("Digital literacy & media");
        amara.setExpertiseTopics("DIGITAL_LITERACY,MEDIA_LITERACY,CRITICAL_THINKING");
        amara.setCountry("Uganda");

        Mentor kato = (Mentor) user(new Mentor(), Role.MENTOR, "Brian Kato", "brian@yep.africa");
        kato.setMentorshipAreas("EMPLOYMENT,TECHNOLOGY,STARTUPS");
        kato.setCountry("Kenya");

        Participant nabirye = participant("Sarah Nabirye", "sarah@yep.africa", "Uganda", 24, SkillLevel.INTERMEDIATE);
        Participant okello = participant("Daniel Okello", "daniel@yep.africa", "Uganda", 27, SkillLevel.BEGINNER);
        Participant achieng = participant("Grace Achieng", "grace@yep.africa", "Kenya", 22, SkillLevel.ADVANCED);
        Participant tesfaye = participant("Mikiyas Tesfaye", "mikiyas@yep.africa", "Ethiopia", 29,
                SkillLevel.INTERMEDIATE);

        userRepository.saveAll(List.of(admin, amara, kato, nabirye, okello, achieng, tesfaye));

        // ---- Level One sessions ------------------------------------------------
        LevelOneExchange digitalLiteracy = new LevelOneExchange();
        digitalLiteracy.setTitle("Digital Literacy Bootcamp: Stay Safe Online");
        digitalLiteracy.setDescription(
                "A practical introduction to passwords, privacy settings and safe browsing for young people entering the digital economy.");
        digitalLiteracy.setLevel(ExchangeLevel.LEVEL_ONE);
        digitalLiteracy.setTopic(LevelOneTopic.DIGITAL_LITERACY);
        digitalLiteracy.setStartDate(LocalDate.now().plusDays(7));
        digitalLiteracy.setEndDate(LocalDate.now().plusDays(8));
        digitalLiteracy.setStatus(ExchangeStatus.OPEN);
        digitalLiteracy.setMaxParticipants(60);
        digitalLiteracy.setSessionLink("https://meet.example.org/yep-digital-literacy");
        digitalLiteracy.setFacilitatorId(amara.getId());

        LevelOneExchange mediaLiteracy = new LevelOneExchange();
        mediaLiteracy.setTitle("Media Literacy: Spotting Misinformation");
        mediaLiteracy.setDescription(
                "Learn fact-checking techniques, source evaluation and how disinformation spreads on social platforms.");
        mediaLiteracy.setLevel(ExchangeLevel.LEVEL_ONE);
        mediaLiteracy.setTopic(LevelOneTopic.MEDIA_LITERACY);
        mediaLiteracy.setStartDate(LocalDate.now().plusDays(14));
        mediaLiteracy.setEndDate(LocalDate.now().plusDays(14));
        mediaLiteracy.setStatus(ExchangeStatus.OPEN);
        mediaLiteracy.setMaxParticipants(80);
        mediaLiteracy.setSessionLink("https://meet.example.org/yep-media-literacy");
        mediaLiteracy.setFacilitatorId(amara.getId());

        levelOneRepository.saveAll(List.of(digitalLiteracy, mediaLiteracy));

        // ---- Level Two programme ----------------------------------------------
        LevelTwoExchange employment = new LevelTwoExchange();
        employment.setTitle("Impact Lab: Youth Employment Solutions");
        employment.setDescription(
                "Small teams design and test solutions that get young people into decent work across East Africa.");
        employment.setLevel(ExchangeLevel.LEVEL_TWO);
        employment.setFocusArea(LevelTwoFocusArea.EMPLOYMENT);
        employment.setStartDate(LocalDate.now().plusDays(3));
        employment.setEndDate(LocalDate.now().plusDays(60));
        employment.setStatus(ExchangeStatus.OPEN);
        employment.setMaxTeams(10);
        employment.setApplicationsOpen(true);
        levelTwoRepository.save(employment);

        // ---- Enrollments -------------------------------------------------------
        for (Participant participant : List.of(nabirye, okello, achieng, tesfaye)) {
            enrollmentRepository.save(Enrollment.open(participant, digitalLiteracy));
        }
        Enrollment nabiryeMedia = Enrollment.open(nabirye, mediaLiteracy);
        enrollmentRepository.save(nabiryeMedia);
        for (Participant participant : List.of(nabirye, okello, achieng)) {
            enrollmentRepository.save(Enrollment.open(participant, employment));
        }

        // ---- Team --------------------------------------------------------------
        Team team = new Team();
        team.setName("Kampala Works");
        team.setDescription("Peer-to-peer job matching platform for first-time job seekers.");
        team.setLevelTwoExchange(employment);
        team.setMentor(kato);
        team = teamRepository.save(team);

        teamMemberRepository.save(team.addMember(nabirye, TeamMemberRole.LEAD));
        teamMemberRepository.save(team.addMember(okello, TeamMemberRole.MEMBER));
        teamMemberRepository.save(team.addMember(achieng, TeamMemberRole.MEMBER));
        team = teamRepository.save(team);

        ImpactProject project = ImpactProject.draftFor(team, "Kampala Works: Peer Job Matching Pilot");
        project.setProblemStatement(
                "Graduates in Kampala rely on word of mouth and paid job boards that exclude informal workers.");
        project.setProposedSolution(
                "A USSD + lightweight web directory that matches verified employers with youth in their parish, free for job seekers.");
        project.setAiAssisted(true);
        project.setAiUsageNote("AI used to draft the problem statement; facilitator edited and verified all statistics.");
        project.submit();
        project.startReview();
        project = projectRepository.save(project);
        team.setImpactProject(project);
        teamRepository.save(team);

        // ---- Assessments -------------------------------------------------------
        assessmentRepository.save(Assessment.of(nabirye, amara, "Digital literacy participation", 86,
                "Excellent questioning and strong peer support during the breakout rooms."));
        assessmentRepository.save(Assessment.of(team, amara, "Impact project - clarity of problem", 78,
                "Clear problem statement; next iteration should add measurable success indicators."));

        // ---- Certificate -------------------------------------------------------
        if (nabiryeMedia.getCompletionStatus() == CompletionStatus.IN_PROGRESS) {
            nabiryeMedia.markCompleted();
            enrollmentRepository.save(nabiryeMedia);
        }
        certificateRepository.save(Certificate.issuedTo(nabirye,
                "Certificate of Participation - Sarah Nabirye",
                "Media Literacy: Spotting Misinformation",
                "YEP-demo-certificate-code"));

        // ---- Announcement ------------------------------------------------------
        Announcement announcement = Announcement.byUser(amara,
                com.yep.domain.enums.AnnouncementScope.ALL_USERS,
                "Welcome to the October cohort",
                "Orientation starts next Monday. Bring a phone or laptop and a curious mind.",
                false);
        messageRepository.save(announcement);

        log.info("Demo data seeded: {} users, {} exchanges, 1 team, 1 project",
                userRepository.count(), levelOneRepository.count() + levelTwoRepository.count());
    }

    private User user(User entity, Role role, String name, String email) {
        entity.setRole(role);
        entity.setFullName(name);
        entity.setEmail(email);
        entity.setPasswordHash(passwordEncoder.encode("Password123!"));
        entity.setCountry("Uganda");
        entity.setConsentGiven(true);
        entity.setAccountStatus(AccountStatus.ACTIVE);
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }

    private Participant participant(String name, String email, String country, int age, SkillLevel skill) {
        Participant participant = new Participant();
        user(participant, Role.PARTICIPANT, name, email);
        participant.setCountry(country);
        participant.setAge(age);
        participant.setSkillLevel(skill);
        return participant;
    }
}
