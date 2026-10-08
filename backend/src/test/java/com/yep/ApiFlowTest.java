package com.yep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end API smoke test: register -&gt; login -&gt; create activity -&gt;
 * enroll -&gt; team -&gt; project, plus the public certificate verification and
 * the role/authorization rules.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static String participantToken;
    private static String facilitatorToken;
    private static Long levelOneId;
    private static Long levelTwoId;
    private static Long teamId;
    private static Long projectId;

    @Test
    @Order(1)
    void participantCanRegister() throws Exception {
        String body = """
                {
                  "fullName": "Flow Participant",
                  "email": "%s",
                  "password": "Password123!",
                  "country": "Uganda",
                  "role": "PARTICIPANT",
                  "consentGiven": true,
                  "age": 24,
                  "skillLevel": "INTERMEDIATE"
                }
                """.formatted(uniqueEmail());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PARTICIPANT"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @Order(2)
    void registrationRequiresConsentAndRejectsAdmin() throws Exception {
        String noConsent = """
                {"fullName":"No Consent","email":"%s","password":"Password123!","role":"PARTICIPANT","consentGiven":false}
                """.formatted(uniqueEmail());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(noConsent))
                .andExpect(status().isBadRequest());

        String admin = """
                {"fullName":"Rogue Admin","email":"%s","password":"Password123!","role":"ADMIN","consentGiven":true}
                """.formatted(uniqueEmail());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void loginReturnsTokenAndHttpOnlyCookie() throws Exception {
        String email = uniqueEmail();
        register(email, "PARTICIPANT");

        String login = "{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        participantToken = node.get("token").asText();
        String setCookie = result.getResponse().getHeader("Set-Cookie");
        assertThat(setCookie).contains("yep_token=");
        assertThat(setCookie.toLowerCase()).contains("httponly");
    }

    @Test
    @Order(4)
    void facilitatorCreatesBothExchangeLevelsAndSearchFindsThem() throws Exception {
        String email = uniqueEmail();
        register(email, "FACILITATOR");
        facilitatorToken = login(email);

        String levelOne = """
                {
                  "title": "API Flow Digital Literacy",
                  "description": "Learn to browse safely",
                  "level": "LEVEL_ONE",
                  "topic": "DIGITAL_LITERACY",
                  "startDate": "2027-01-10",
                  "endDate": "2027-01-11",
                  "maxParticipants": 25,
                  "sessionLink": "https://meet.example.org/flow"
                }
                """;
        MvcResult created = mockMvc.perform(post("/api/v1/exchanges")
                        .header("Authorization", "Bearer " + facilitatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(levelOne))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value("LEVEL_ONE"))
                .andExpect(jsonPath("$.topic").value("DIGITAL_LITERACY"))
                .andReturn();
        levelOneId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String levelTwo = """
                {
                  "title": "API Flow Impact Lab",
                  "description": "Teams solving real problems",
                  "level": "LEVEL_TWO",
                  "focusArea": "EMPLOYMENT",
                  "startDate": "2027-02-01",
                  "endDate": "2027-04-01",
                  "maxTeams": 8
                }
                """;
        MvcResult createdTwo = mockMvc.perform(post("/api/v1/exchanges")
                        .header("Authorization", "Bearer " + facilitatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(levelTwo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value("LEVEL_TWO"))
                .andReturn();
        levelTwoId = objectMapper.readTree(createdTwo.getResponse().getContentAsString()).get("id").asLong();

        // public search with filters
        mockMvc.perform(get("/api/v1/exchanges")
                        .param("level", "LEVEL_ONE")
                        .param("topic", "DIGITAL_LITERACY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].level").value("LEVEL_ONE"));

        mockMvc.perform(get("/api/v1/exchanges")
                        .param("q", "Impact Lab")
                        .param("focusArea", "EMPLOYMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].focusArea").value("EMPLOYMENT"));
    }

    @Test
    @Order(5)
    void anonymousWriteIsRejectedButPublicReadIsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/exchanges"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/exchanges")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"level\":\"LEVEL_ONE\",\"startDate\":\"2027-01-01\"}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void participantEnrollsOnceOnly() throws Exception {
        mockMvc.perform(post("/api/v1/exchanges/" + levelOneId + "/enroll")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completionStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.level").value("LEVEL_ONE"));

        mockMvc.perform(post("/api/v1/exchanges/" + levelOneId + "/enroll")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(7)
    void participantFormsTeamAndSubmitsImpactProject() throws Exception {
        String team = """
                {"name":"Flow Team %s","description":"API flow team","levelTwoExchangeId":%d}
                """.formatted(UUID.randomUUID().toString().substring(0, 8), levelTwoId);

        MvcResult created = mockMvc.perform(post("/api/v1/teams")
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(team))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.members[0].memberRole").value("LEAD"))
                .andReturn();
        teamId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        String project = """
                {
                  "title": "Flow Job Board",
                  "problemStatement": "Youth cannot find verified employers in their parish.",
                  "proposedSolution": "A free USSD directory matching verified employers with local youth.",
                  "aiAssisted": false
                }
                """;
        MvcResult projectCreated = mockMvc.perform(post("/api/v1/projects?teamId=" + teamId)
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(project))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();
        projectId = objectMapper.readTree(projectCreated.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/submit")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @Order(8)
    void facilitatorReviewsProjectAndAssessesTeam() throws Exception {
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/review?decision=START")
                        .header("Authorization", "Bearer " + facilitatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));

        mockMvc.perform(post("/api/v1/projects/" + projectId + "/review?decision=APPROVE")
                        .header("Authorization", "Bearer " + facilitatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        String assessment = """
                {"targetType":"TEAM","teamId":%d,"criteria":"Clarity of the proposed solution","score":88,
                 "feedback":"Strong local insight; add a rollout plan."}
                """.formatted(teamId);
        mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", "Bearer " + facilitatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assessment))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.targetType").value("TEAM"))
                .andExpect(jsonPath("$.targetId").value(teamId))
                .andExpect(jsonPath("$.score").value(88))
                .andExpect(jsonPath("$.pass").value(true));
    }

    @Test
    @Order(9)
    void participantCannotUseStaffEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/assessments")
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetType\":\"TEAM\",\"teamId\":1,\"criteria\":\"x\",\"score\":50}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/ai/suggest")
                        .header("Authorization", "Bearer " + participantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"draft an announcement\",\"kind\":\"announcement\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(10)
    void completingProjectIssuesVerifiableCertificates() throws Exception {
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/complete")
                        .header("Authorization", "Bearer " + facilitatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        MvcResult certificates = mockMvc.perform(get("/api/v1/certificates")
                        .header("Authorization", "Bearer " + facilitatorToken))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode list = objectMapper.readTree(certificates.getResponse().getContentAsString());
        assertThat(list.isArray()).isTrue();
        assertThat(list.size()).isGreaterThan(0);

        String code = list.get(0).get("verificationCode").asText();
        mockMvc.perform(get("/api/v1/certificates/verify/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.recipientName").isNotEmpty());

        mockMvc.perform(get("/api/v1/certificates/verify/YEP-does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    @Order(11)
    void aiDraftedAnnouncementWaitsForHumanApproval() throws Exception {
        String announcement = """
                {
                  "subject": "Orientation reminder",
                  "body": "Orientation starts at 10:00 EAT.",
                  "scope": "ALL_USERS",
                  "aiGenerated": true
                }
                """;
        MvcResult created = mockMvc.perform(post("/api/v1/messages/announcements")
                        .header("Authorization", "Bearer " + facilitatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(announcement))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresHumanApproval").value(true))
                .andExpect(jsonPath("$.published").value(false))
                .andReturn();

        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/messages/announcements/" + id + "/approve")
                        .header("Authorization", "Bearer " + facilitatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.requiresHumanApproval").value(false));
    }

    @Test
    @Order(12)
    void messagingBetweenParticipantsWorks() throws Exception {
        String secondEmail = uniqueEmail();
        register(secondEmail, "PARTICIPANT");
        String secondToken = login(secondEmail);
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(secondEmail));

        mockMvc.perform(post("/api/v1/messages")
                        .header("Authorization", "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"Hello\",\"body\":\"Karibu to YEP!\"}"))
                .andExpect(status().isBadRequest()); // no recipient -> rejected

        mockMvc.perform(get("/api/v1/messages")
                        .header("Authorization", "Bearer " + participantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // helpers -------------------------------------------------------------

    private String uniqueEmail() {
        return "flow-" + UUID.randomUUID().toString().substring(0, 12) + "@yep.test";
    }

    private void register(String email, String role) throws Exception {
        String body = """
                {"fullName":"Flow User","email":"%s","password":"Password123!","country":"Uganda",
                 "role":"%s","consentGiven":true,"age":23,"expertiseArea":"Digital literacy"}
                """.formatted(email, role);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
