package com.yep.config;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.activity.LearningActivity;
import com.yep.domain.activity.LevelOneActivity;
import com.yep.domain.activity.LevelTwoActivity;
import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.ActivityStatus;
import com.yep.domain.enums.ChallengeArea;
import com.yep.domain.enums.EnrollmentStatus;
import com.yep.domain.enums.MaterialType;
import com.yep.domain.enums.MilestoneStatus;
import com.yep.domain.enums.OrganisationType;
import com.yep.domain.enums.ProjectStatus;
import com.yep.domain.enums.TaskType;
import com.yep.domain.enums.TeamMemberRole;
import com.yep.domain.enums.TeamStatus;
import com.yep.domain.learning.AssessmentTask;
import com.yep.domain.learning.DiscussionPost;
import com.yep.domain.learning.DiscussionThread;
import com.yep.domain.learning.Enrollment;
import com.yep.domain.learning.LearningMaterial;
import com.yep.domain.learning.VirtualSession;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.project.Milestone;
import com.yep.domain.project.Team;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.PartnerOrganisation;
import com.yep.domain.user.YouthParticipant;
import com.yep.repository.AssessmentTaskRepository;
import com.yep.repository.DiscussionPostRepository;
import com.yep.repository.DiscussionThreadRepository;
import com.yep.repository.EnrollmentRepository;
import com.yep.repository.FacilitatorRepository;
import com.yep.repository.ImpactProjectRepository;
import com.yep.repository.LearningActivityRepository;
import com.yep.repository.LearningMaterialRepository;
import com.yep.repository.MentorRepository;
import com.yep.repository.MilestoneRepository;
import com.yep.repository.PartnerOrganisationRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.repository.VirtualSessionRepository;
import com.yep.repository.YouthParticipantRepository;

/**
 * Demo seed data for presentations. Activated via SPRING_PROFILES_ACTIVE=prod,demo.
 * Idempotent: will not seed if users already exist beyond the initial admin.
 *
 * Demo accounts (password for all: Demo1234!):
 *   facilitator@yep.demo – Facilitator
 *   mentor@yep.demo      – Mentor
 *   alice@yep.demo       – Participant (Uganda, Level-Two-eligible)
 *   bob@yep.demo         – Participant (Kenya, Level-Two-eligible)
 *   carol@yep.demo       – Participant (Tanzania)
 *   david@yep.demo       – Participant (Rwanda)
 *   eve@yep.demo         – Participant (Uganda)
 *   frank@yep.demo       – Participant (Kenya)
 *   university@yep.demo  – University partner (Makerere University)
 *   employer@yep.demo    – Employer partner (TechHub EA)
 */
@Component
@Profile("demo")
public class DemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(DemoSeeder.class);

    private final UserRepository userRepository;
    private final FacilitatorRepository facilitatorRepository;
    private final MentorRepository mentorRepository;
    private final YouthParticipantRepository youthRepository;
    private final PartnerOrganisationRepository partnerRepository;
    private final LearningActivityRepository activityRepository;
    private final LearningMaterialRepository materialRepository;
    private final VirtualSessionRepository sessionRepository;
    private final AssessmentTaskRepository taskRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeamRepository teamRepository;
    private final ImpactProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final DiscussionThreadRepository threadRepository;
    private final DiscussionPostRepository postRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoSeeder(UserRepository userRepository, FacilitatorRepository facilitatorRepository,
            MentorRepository mentorRepository, YouthParticipantRepository youthRepository,
            PartnerOrganisationRepository partnerRepository, LearningActivityRepository activityRepository,
            LearningMaterialRepository materialRepository, VirtualSessionRepository sessionRepository,
            AssessmentTaskRepository taskRepository, EnrollmentRepository enrollmentRepository,
            TeamRepository teamRepository, ImpactProjectRepository projectRepository,
            MilestoneRepository milestoneRepository, DiscussionThreadRepository threadRepository,
            DiscussionPostRepository postRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.facilitatorRepository = facilitatorRepository;
        this.mentorRepository = mentorRepository;
        this.youthRepository = youthRepository;
        this.partnerRepository = partnerRepository;
        this.activityRepository = activityRepository;
        this.materialRepository = materialRepository;
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.teamRepository = teamRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.threadRepository = threadRepository;
        this.postRepository = postRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        // Idempotent: skip if demo data exists (more than 1 user = admin was seeded)
        if (userRepository.count() > 1) {
            log.info("Demo: data already present, skipping seed.");
            return;
        }
        log.info("Demo: seeding demo data…");

        String hash = passwordEncoder.encode("Demo1234!");

        // ---- Facilitator ----
        Facilitator facilitator = new Facilitator();
        facilitator.setName("Dr. Grace Omondi");
        facilitator.setEmail("facilitator@yep.demo");
        facilitator.setPassword(hash);
        facilitator.setStatus(AccountStatus.ACTIVE);
        facilitator.setSpecialisation("Digital Literacy and AI Education");
        facilitator.setPhone("+256700000001");
        facilitator = facilitatorRepository.save(facilitator);

        // ---- Mentor ----
        Mentor mentor = new Mentor();
        mentor.setName("James Mwangi");
        mentor.setEmail("mentor@yep.demo");
        mentor.setPassword(hash);
        mentor.setStatus(AccountStatus.ACTIVE);
        mentor.setOrganisation("Kenya Digital Foundation");
        mentor.setExpertise("Tech Entrepreneurship, Digital Inclusion");
        mentor.setPhone("+254700000001");
        mentor = mentorRepository.save(mentor);

        // ---- Participants ----
        YouthParticipant alice = makeParticipant("Alice Nakato", "alice@yep.demo", hash, 24, "Uganda", "Advanced", true);
        YouthParticipant bob   = makeParticipant("Bob Ochieng",  "bob@yep.demo",   hash, 26, "Kenya",   "Intermediate", true);
        YouthParticipant carol = makeParticipant("Carol Uwimana","carol@yep.demo", hash, 22, "Tanzania","Beginner", false);
        YouthParticipant david = makeParticipant("David Nkusi",  "david@yep.demo", hash, 28, "Rwanda",  "Intermediate", false);
        YouthParticipant eve   = makeParticipant("Eve Apio",     "eve@yep.demo",   hash, 20, "Uganda",  "Beginner", false);
        YouthParticipant frank = makeParticipant("Frank Kamau",  "frank@yep.demo", hash, 23, "Kenya",   "Advanced", false);

        // ---- University Partner ----
        PartnerOrganisation university = new PartnerOrganisation();
        university.setName("Makerere University Digital Lab");
        university.setEmail("university@yep.demo");
        university.setPassword(hash);
        university.setStatus(AccountStatus.ACTIVE);
        university.setOrganisationName("Makerere University");
        university.setCountry("Uganda");
        university.setOrganisationType(OrganisationType.UNIVERSITY);
        university.setWebsite("https://makerere.ac.ug");
        university.setDescription("Uganda's premier public university partnering on digital literacy research.");
        university.setVerified(true);
        university = partnerRepository.save(university);

        // ---- Employer Partner ----
        PartnerOrganisation employer = new PartnerOrganisation();
        employer.setName("TechHub East Africa");
        employer.setEmail("employer@yep.demo");
        employer.setPassword(hash);
        employer.setStatus(AccountStatus.ACTIVE);
        employer.setOrganisationName("TechHub EA");
        employer.setCountry("Kenya");
        employer.setOrganisationType(OrganisationType.EMPLOYER);
        employer.setWebsite("https://techhubea.example.com");
        employer.setDescription("Leading tech startup hub in Nairobi supporting youth employment.");
        employer.setVerified(true);
        employer = partnerRepository.save(employer);

        // ---- Level One Activity: AI Literacy ----
        LevelOneActivity aiActivity = new LevelOneActivity();
        aiActivity.setTitle("AI Literacy for Young Innovators");
        aiActivity.setDescription("Understand how AI works, its benefits and risks, and use it responsibly in your daily life and projects.");
        aiActivity.setCategory("DIGITAL_LITERACY");
        aiActivity.setStartDate(LocalDate.now().minusDays(10));
        aiActivity.setEndDate(LocalDate.now().plusDays(20));
        aiActivity.setStatus(ActivityStatus.PUBLISHED);
        aiActivity.setFacilitator(facilitator);
        aiActivity.setPartner(university);
        aiActivity.setCapacity(50);
        aiActivity.setPassMark(60);
        aiActivity.setMinAttendancePercent(70);
        aiActivity = (LevelOneActivity) activityRepository.save(aiActivity);

        seedMaterials(aiActivity, facilitator, "AI Literacy");
        seedSessions(aiActivity, facilitator, "AI Literacy");
        seedTasks(aiActivity, facilitator, "AI Literacy");
        seedGeneralThread(aiActivity, facilitator);

        // ---- Level One Activity: Media Literacy ----
        LevelOneActivity mediaActivity = new LevelOneActivity();
        mediaActivity.setTitle("Media Literacy and Critical Thinking");
        mediaActivity.setDescription("Learn to identify misinformation, understand bias and communicate effectively in a digital world.");
        mediaActivity.setCategory("CRITICAL_THINKING");
        mediaActivity.setStartDate(LocalDate.now().minusDays(5));
        mediaActivity.setEndDate(LocalDate.now().plusDays(25));
        mediaActivity.setStatus(ActivityStatus.PUBLISHED);
        mediaActivity.setFacilitator(facilitator);
        mediaActivity.setCapacity(40);
        mediaActivity.setPassMark(60);
        mediaActivity.setMinAttendancePercent(70);
        mediaActivity = (LevelOneActivity) activityRepository.save(mediaActivity);

        seedMaterials(mediaActivity, facilitator, "Media Literacy");
        seedSessions(mediaActivity, facilitator, "Media Literacy");
        seedTasks(mediaActivity, facilitator, "Media Literacy");
        seedGeneralThread(mediaActivity, facilitator);

        // ---- Enroll participants in AI Literacy ----
        for (YouthParticipant p : List.of(alice, bob, carol, david, eve, frank)) {
            // Encapsulation: status transitions happen through the domain methods.
            Enrollment enrollment = new Enrollment(p, aiActivity);
            enrollment.confirmEnrollment();
            enrollment.setEnrollmentDate(OffsetDateTime.now().minusDays(8));
            enrollmentRepository.save(enrollment);
        }

        // ---- Level Two Activity: Digital Inclusion ----
        LevelTwoActivity l2Activity = new LevelTwoActivity();
        l2Activity.setTitle("Digital Inclusion Impact Programme");
        l2Activity.setDescription("Form teams and develop impactful projects addressing digital exclusion in East Africa.");
        l2Activity.setCategory("DIGITAL_INCLUSION");
        l2Activity.setStartDate(LocalDate.now().minusDays(3));
        l2Activity.setEndDate(LocalDate.now().plusDays(60));
        l2Activity.setStatus(ActivityStatus.PUBLISHED);
        l2Activity.setFacilitator(facilitator);
        l2Activity.setCapacity(20);
        l2Activity.setPassMark(60);
        l2Activity.setMinAttendancePercent(60);
        l2Activity.setMinTeamSize(2);
        l2Activity.setMaxTeamSize(4);
        l2Activity = (LevelTwoActivity) activityRepository.save(l2Activity);

        // ---- Enroll level-two-eligible participants ----
        for (YouthParticipant p : List.of(alice, bob)) {
            Enrollment e = new Enrollment(p, l2Activity);
            e.confirmEnrollment();
            e.setEnrollmentDate(OffsetDateTime.now().minusDays(2));
            enrollmentRepository.save(e);
        }

        // ---- Team ----
        Team team = new Team(l2Activity, "Innovators Afrika", alice);
        team.setMentor(mentor);
        team.setStatus(TeamStatus.OPEN);
        team.addMember(alice, TeamMemberRole.LEADER);
        team.addMember(bob, TeamMemberRole.MEMBER);
        team.refreshStatus(l2Activity.getMaxTeamSize());
        team = teamRepository.save(team);

        // ---- Private discussion thread for the team ----
        DiscussionThread teamThread = new DiscussionThread(l2Activity, team, "Innovators Afrika – Team Chat");
        teamThread.setCreatedAt(OffsetDateTime.now().minusDays(2));
        teamThread = threadRepository.save(teamThread);

        DiscussionPost post1 = new DiscussionPost(teamThread, alice,
                "Welcome to the team! Let's create something impactful for digital inclusion.");
        post1.setPostedAt(OffsetDateTime.now().minusDays(2));
        postRepository.save(post1);

        DiscussionPost post2 = new DiscussionPost(teamThread, bob,
                "Excited to work with everyone! I have some ideas on mobile data access in rural Kenya.");
        post2.setPostedAt(OffsetDateTime.now().minusDays(1));
        postRepository.save(post2);

        // ---- Impact Project ----
        ImpactProject project = new ImpactProject(team, "Rural Mobile Data Access Initiative");
        project.setDescription("Design a community Wi-Fi hotspot model for rural East Africa to bridge the digital divide.");
        project.setChallengeArea(ChallengeArea.DIGITAL_INCLUSION);
        project.setStatus(ProjectStatus.DRAFT);
        project.setAiUsed(true);
        project.setAiUsageDeclaration("Used ChatGPT to draft the problem statement. The team reviewed and rewrote it to reflect our local context.");
        project.setOpenForSupport(false);
        project = projectRepository.save(project);

        // ---- Milestones ----
        for (String[] ms : new String[][] {
                {"Research and needs assessment", "14"},
                {"Solution design and prototype", "30"},
                {"Pilot test and report", "55"}
        }) {
            Milestone m = new Milestone(project, ms[0], LocalDate.now().plusDays(Long.parseLong(ms[1])));
            m.setStatus(MilestoneStatus.PENDING);
            milestoneRepository.save(m);
        }

        log.info("Demo: seed complete. {} users now in DB.", userRepository.count());
    }

    private YouthParticipant makeParticipant(String name, String email, String hash,
            int age, String country, String skills, boolean levelTwoEligible) {
        YouthParticipant p = new YouthParticipant();
        p.setName(name);
        p.setEmail(email);
        p.setPassword(hash);
        p.setStatus(AccountStatus.ACTIVE);
        p.setAge(age);
        p.setCountry(country);
        p.setDigitalSkills(skills);
        p.setLevelTwoEligible(levelTwoEligible);
        p.setShareProfileWithPartners(true);
        p.setConsentDate(OffsetDateTime.now().minusDays(10));
        p.setInstitution("Makerere University");
        return youthRepository.save(p);
    }

    private void seedMaterials(LearningActivity activity, com.yep.domain.user.User uploader, String prefix) {
        String[][] materials = {
                {"Introduction to " + prefix, "VIDEO", "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "Watch this introduction video before the first session."},
                {prefix + " Slide Deck", "SLIDES", "https://docs.google.com/presentation/d/example", "Session slides — download before the live session."},
                {"Supplementary Reading", "LINK", "https://en.wikipedia.org/wiki/Digital_literacy", "Optional deeper reading for motivated learners."},
        };
        for (String[] m : materials) {
            LearningMaterial mat = new LearningMaterial(activity, m[0], MaterialType.valueOf(m[1]), m[2], uploader);
            mat.setDescription(m[3]);
            mat.setUploadedAt(OffsetDateTime.now().minusDays(8));
            mat.setPublished(true);
            materialRepository.save(mat);
        }
    }

    private void seedSessions(LearningActivity activity, com.yep.domain.user.User creator, String prefix) {
        for (int i = 1; i <= 2; i++) {
            VirtualSession session = new VirtualSession(activity, prefix + " – Session " + i,
                    OffsetDateTime.now().plusDays(i * 7L), 90,
                    "https://meet.google.com/demo-" + prefix.toLowerCase().replace(' ', '-') + "-" + i, creator);
            sessionRepository.save(session);
        }
    }

    private void seedTasks(LearningActivity activity, com.yep.domain.user.User creator, String prefix) {
        // Quiz
        AssessmentTask quiz = new AssessmentTask(activity, prefix + " Quiz", TaskType.QUIZ);
        quiz.setInstructions("Answer all questions. Each correct answer earns 2 points.");
        quiz.setMaxScore(10);
        quiz.setDueDate(OffsetDateTime.now().plusDays(14));
        quiz.setPublished(true);
        quiz.setQuestions(List.of(
            Map.of("text", "What does AI stand for?", "options", List.of("Artificial Intelligence","Automated Input","Advanced Interface","None"), "correctIndex", 0),
            Map.of("text", "Which of these is NOT a type of machine learning?", "options", List.of("Supervised","Unsupervised","Reinforcement","Sequential"), "correctIndex", 3),
            Map.of("text", "What is a neural network modelled after?", "options", List.of("Computer hardware","The human brain","A telephone network","Database tables"), "correctIndex", 1),
            Map.of("text", "What is 'training data' in AI?", "options", List.of("Data used to test the model","Data used to teach the model","Data encrypted for security","Data from training videos"), "correctIndex", 1),
            Map.of("text", "What is bias in AI?", "options", List.of("A type of data format","Unfair outcomes due to skewed data","A programming language feature","A hardware component"), "correctIndex", 1)
        ));
        taskRepository.save(quiz);

        // Assignment
        AssessmentTask assignment = new AssessmentTask(activity, prefix + " Reflection Assignment", TaskType.ASSIGNMENT);
        assignment.setInstructions("Write 300–500 words reflecting on how " + prefix.toLowerCase() + " affects your community. Submit your essay below.");
        assignment.setMaxScore(20);
        assignment.setDueDate(OffsetDateTime.now().plusDays(21));
        assignment.setPublished(true);
        taskRepository.save(assignment);
    }

    /** Every published activity gets a default "General" discussion thread (business rule). */
    private void seedGeneralThread(LearningActivity activity, com.yep.domain.user.User facilitator) {
        DiscussionThread generalThread = new DiscussionThread(activity, "General");
        generalThread.setCreatedAt(OffsetDateTime.now().minusDays(9));
        generalThread = threadRepository.save(generalThread);

        DiscussionPost welcomePost = new DiscussionPost(generalThread, facilitator,
                "Welcome to " + activity.getTitle() + "! Please introduce yourself and share what you hope to learn.");
        welcomePost.setPostedAt(OffsetDateTime.now().minusDays(9));
        postRepository.save(welcomePost);
    }
}
