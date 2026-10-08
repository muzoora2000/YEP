package com.yep;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.assessment.Assessment;
import com.yep.domain.certificate.Certificate;
import com.yep.domain.enums.AssessmentTargetType;
import com.yep.domain.enums.LevelOneTopic;
import com.yep.domain.enums.LevelTwoFocusArea;
import com.yep.domain.enums.Role;
import com.yep.domain.exchange.LevelOneExchange;
import com.yep.domain.exchange.LevelTwoExchange;
import com.yep.domain.exchange.VirtualExchange;
import com.yep.domain.project.ImpactProject;
import com.yep.domain.team.Team;
import com.yep.domain.team.TeamMember;
import com.yep.domain.user.Admin;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;
import com.yep.repository.AssessmentRepository;
import com.yep.repository.CertificateRepository;
import com.yep.repository.TeamMemberRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.repository.VirtualExchangeRepository;

/**
 * Boots the whole application against the real Flyway migrations with
 * {@code ddl-auto=validate}. If the SQL schema and the JPA entities ever drift
 * apart, this test fails at context start-up - which is exactly what would
 * happen on Render.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SchemaValidationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VirtualExchangeRepository exchangeRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Test
    void flywaySchemaMatchesJpaEntities() {
        // Reaching this assertion means Hibernate validated every table and column.
        assertThat(userRepository).isNotNull();
        assertThat(exchangeRepository).isNotNull();
    }

    @Test
    void singleTableUserInheritanceKeepsTheRoleDiscriminator() {
        Participant participant = new Participant();
        participant.setRole(Role.PARTICIPANT);
        participant.setFullName("Schema Tester");
        participant.setEmail("schema-participant@yep.test");
        participant.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        participant.setConsentGiven(true);
        participant.setAge(25);
        userRepository.save(participant);

        Facilitator facilitator = new Facilitator();
        facilitator.setRole(Role.FACILITATOR);
        facilitator.setFullName("Schema Facilitator");
        facilitator.setEmail("schema-facilitator@yep.test");
        facilitator.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        facilitator.setConsentGiven(true);
        facilitator.setExpertiseArea("AI literacy");
        userRepository.save(facilitator);

        Mentor mentor = new Mentor();
        mentor.setRole(Role.MENTOR);
        mentor.setFullName("Schema Mentor");
        mentor.setEmail("schema-mentor@yep.test");
        mentor.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        mentor.setConsentGiven(true);
        mentor.setMentorshipAreas("HEALTH,CLIMATE");
        userRepository.save(mentor);

        Admin admin = new Admin();
        admin.setRole(Role.ADMIN);
        admin.setFullName("Schema Admin");
        admin.setEmail("schema-admin@yep.test");
        admin.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        admin.setConsentGiven(true);
        userRepository.save(admin);

        // Polymorphic read: one query, four concrete types.
        List<User> all = userRepository.findAll();
        assertThat(all).isNotEmpty();
        assertThat(all.stream().filter(Participant.class::isInstance)).isNotEmpty();
        assertThat(all.stream().filter(Admin.class::isInstance)).isNotEmpty();

        // The mapped discriminator column really holds the role.
        Optional<User> loaded = userRepository.findByEmail("schema-facilitator@yep.test");
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getRole()).isEqualTo(Role.FACILITATOR);
        assertThat(loaded.get()).isInstanceOf(Facilitator.class);

        // ... and role based queries work.
        assertThat(userRepository.findByRole(Role.ADMIN)).isNotEmpty();
    }

    @Test
    void nativeSearchHydratesBothExchangeLevels() {
        LevelOneExchange levelOne = new LevelOneExchange();
        levelOne.setTitle("Searchable Level One session");
        levelOne.setDescription("Polymorphic hydration check");
        levelOne.setTopic(LevelOneTopic.AI_LITERACY);
        levelOne.setStartDate(LocalDate.now().plusDays(3));
        exchangeRepository.save(levelOne);

        LevelTwoExchange levelTwo = new LevelTwoExchange();
        levelTwo.setTitle("Searchable Level Two programme");
        levelTwo.setDescription("Polymorphic hydration check");
        levelTwo.setFocusArea(LevelTwoFocusArea.CLIMATE);
        levelTwo.setStartDate(LocalDate.now().plusDays(4));
        exchangeRepository.save(levelTwo);

        List<VirtualExchange> results = exchangeRepository.findAllById(exchangeRepository
                .searchIds(null, null, null, null, "Polymorphic hydration", null, null,
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent());

        assertThat(results).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results).allSatisfy(result -> assertThat(result).isInstanceOf(VirtualExchange.class));
        assertThat(results).anyMatch(LevelOneExchange.class::isInstance);
        assertThat(results).anyMatch(LevelTwoExchange.class::isInstance);

        // Level filtered search uses the discriminator column.
        List<VirtualExchange> levelOnes = exchangeRepository.findAllById(exchangeRepository
                .searchIds("LEVEL_ONE", "AI_LITERACY", null, null, null, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 20))
                .getContent());
        assertThat(levelOnes).isNotEmpty();
        assertThat(levelOnes).allMatch(LevelOneExchange.class::isInstance);
    }

    @Test
    void assessmentBindsThePolymorphicTargetToOneForeignKey() {
        Participant target = new Participant();
        target.setRole(Role.PARTICIPANT);
        target.setFullName("Assessed Participant");
        target.setEmail("assessed@yep.test");
        target.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        target.setConsentGiven(true);
        userRepository.save(target);

        Facilitator assessor = new Facilitator();
        assessor.setRole(Role.FACILITATOR);
        assessor.setFullName("Assessing Facilitator");
        assessor.setEmail("assessor@yep.test");
        assessor.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        assessor.setConsentGiven(true);
        userRepository.save(assessor);

        Assessment assessment = Assessment.of(target, assessor, "Participation", 91, "Great contribution");
        assessmentRepository.save(assessment);

        Assessment reloaded = assessmentRepository.findById(assessment.getId()).orElseThrow();
        assertThat(reloaded.getTargetType()).isEqualTo(AssessmentTargetType.PARTICIPANT);
        assertThat(reloaded.getParticipantTarget()).isNotNull();
        assertThat(reloaded.getTeamTarget()).isNull();
        assertThat(reloaded.resolveTarget()).isSameAs(reloaded.getParticipantTarget());
        assertThat(reloaded.getAssessor().getId()).isEqualTo(assessor.getId());
    }

    @Test
    void certificatesStoreAndVerifyThroughTheirPublicCode() {
        Participant holder = new Participant();
        holder.setRole(Role.PARTICIPANT);
        holder.setFullName("Certificate Holder");
        holder.setEmail("holder@yep.test");
        holder.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        holder.setConsentGiven(true);
        userRepository.save(holder);

        Certificate certificate = Certificate.issuedTo(holder, "Certificate of Participation - Certificate Holder",
                "Digital Literacy Bootcamp", "YEP-VERIFY-ME-123");
        certificateRepository.save(certificate);

        assertThat(certificateRepository.existsByVerificationCode("YEP-VERIFY-ME-123")).isTrue();
        assertThat(certificateRepository.findByVerificationCode("YEP-VERIFY-ME-123")).isPresent();
        assertThat(certificateRepository.existsForRecipientAndReference(holder.getId(),
                "Digital Literacy Bootcamp")).isTrue();
    }

    @Test
    void teamMembershipIsPersistedThroughItsJoinEntity() {
        LevelTwoExchange exchange = new LevelTwoExchange();
        exchange.setTitle("Team schema check");
        exchange.setFocusArea(LevelTwoFocusArea.EDUCATION);
        exchange.setStartDate(LocalDate.now().plusDays(1));
        exchangeRepository.save(exchange);

        Participant member = new Participant();
        member.setRole(Role.PARTICIPANT);
        member.setFullName("Team Member");
        member.setEmail("teammate@yep.test");
        member.setPasswordHash("{bcrypt}$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinva");
        member.setConsentGiven(true);
        userRepository.save(member);

        Team team = new Team();
        team.setName("Schema Squad " + System.nanoTime());
        team.setLevelTwoExchange(exchange);
        team = teamRepository.save(team);
        TeamMember membership = team.addMember(member, com.yep.domain.enums.TeamMemberRole.LEAD);
        teamMemberRepository.save(membership);
        team = teamRepository.save(team);

        Team reloaded = teamRepository.findById(team.getId()).orElseThrow();
        assertThat(reloaded.memberCount()).isEqualTo(1);
        assertThat(reloaded.isMember(member)).isTrue();

        ImpactProject project = ImpactProject.draftFor(reloaded, "Schema squad project");
        assertThat(project.getCertificateRecipients()).isEmpty(); // project not completed yet
    }
}
