package com.yep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.platform.PasswordResetToken;
import com.yep.domain.user.Administrator;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.repository.PasswordResetTokenRepository;
import com.yep.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Administrator admin;

    @BeforeEach
    void setUpAdmin() {
        admin = new Administrator();
        admin.setName("Test Admin");
        admin.setEmail("admin@yep.test");
        admin.setPassword(passwordEncoder.encode("Admin1234"));
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setDepartment("Programme");
        admin = userRepository.save(admin);
    }

    @Test
    void participantCanRegisterLoginAndReadProfile() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participantJson("amina@yep.test", 24)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("PARTICIPANT"));

        String token = login("amina@yep.test", "Password1");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Amina Test"))
                .andExpect(jsonPath("$.age").value(24))
                .andExpect(jsonPath("$.country").value("Kenya"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void pendingFacilitatorCannotLoginUntilApproved() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facilitatorJson("facilitator@yep.test")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"facilitator@yep.test\",\"password\":\"Password1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_PENDING"));

        User facilitator = userRepository.findByEmailIgnoreCase("facilitator@yep.test").orElseThrow();
        String adminToken = login("admin@yep.test", "Admin1234");
        mockMvc.perform(put("/api/v1/admin/users/" + facilitator.getUserId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(login("facilitator@yep.test", "Password1")).isNotBlank();
    }

    @Test
    void participantCannotUseAdminEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participantJson("participant@yep.test", 21)))
                .andExpect(status().isCreated());
        String token = login("participant@yep.test", "Password1");
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanListUsersWithAndWithoutFilters() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(facilitatorJson("list-fac@yep.test")))
                .andExpect(status().isCreated());

        String adminToken = login("admin@yep.test", "Admin1234");

        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalItems").value(2));

        mockMvc.perform(get("/api/v1/admin/users").param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("PENDING"));

        mockMvc.perform(get("/api/v1/admin/users").param("role", "FACILITATOR")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].role").value("FACILITATOR"));

        mockMvc.perform(get("/api/v1/admin/users").param("q", "list-fac")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void usersEndpointAllowsSelfButForbidsOthers() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participantJson("self@yep.test", 22)))
                .andExpect(status().isCreated());
        String token = login("self@yep.test", "Password1");
        User self = userRepository.findByEmailIgnoreCase("self@yep.test").orElseThrow();

        mockMvc.perform(get("/api/v1/users/" + self.getUserId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("self@yep.test"));

        mockMvc.perform(get("/api/v1/users/" + admin.getUserId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String adminToken = login("admin@yep.test", "Admin1234");
        mockMvc.perform(get("/api/v1/users/" + self.getUserId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("self@yep.test"));
    }

    @Test
    void rejectsUnderageParticipantAndDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participantJson("young@yep.test", 17)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGE"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participantJson("duplicate@yep.test", 22)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participantJson("DUPLICATE@yep.test", 22)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_IN_USE"));
    }

    @Test
    void resetPasswordUsesSingleUseHashedToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(participantJson("reset@yep.test", 25)))
                .andExpect(status().isCreated());
        User user = userRepository.findByEmailIgnoreCase("reset@yep.test").orElseThrow();
        String rawToken = "known-reset-token";
        tokenRepository.save(new PasswordResetToken(user, sha256(rawToken), OffsetDateTime.now().plusHours(1)));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"NewPass123\"}"))
                .andExpect(status().isOk());

        assertThat(login("reset@yep.test", "NewPass123")).isNotBlank();
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + rawToken + "\",\"newPassword\":\"Another123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    void facilitatorCanBeSwitchedToMentorWithoutNewAccount() throws Exception {
        Facilitator facilitator = new Facilitator();
        facilitator.setName("Switch Me");
        facilitator.setEmail("switch@yep.test");
        facilitator.setPassword(passwordEncoder.encode("Password1"));
        facilitator.setStatus(AccountStatus.ACTIVE);
        facilitator.setSpecialisation("Data");
        facilitator = userRepository.save(facilitator);

        String adminToken = login("admin@yep.test", "Admin1234");
        mockMvc.perform(put("/api/v1/admin/users/" + facilitator.getUserId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MENTOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MENTOR"))
                .andExpect(jsonPath("$.userId").value(facilitator.getUserId()));

        User switched = userRepository.findById(facilitator.getUserId()).orElseThrow();
        assertThat(switched.getRole()).isEqualTo(Role.MENTOR);
        assertThat(switched).isNotInstanceOf(Facilitator.class);
    }

    @Test
    void forgotPasswordDoesNotRevealWhetherEmailExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@yep.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If an account exists for that email, a reset link has been sent."));
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    private String participantJson(String email, int age) {
        return """
                {
                  "name": "Amina Test",
                  "email": "%s",
                  "password": "Password1",
                  "role": "PARTICIPANT",
                  "phone": "+254700000000",
                  "age": %d,
                  "country": "Kenya",
                  "digitalSkills": "Beginner",
                  "institution": "YEP Academy",
                  "bio": "Learning digital skills.",
                  "shareProfileWithPartners": true
                }
                """.formatted(email, age);
    }

    private String facilitatorJson(String email) {
        return """
                {
                  "name": "Facilitator Test",
                  "email": "%s",
                  "password": "Password1",
                  "role": "FACILITATOR",
                  "specialisation": "Web development"
                }
                """.formatted(email);
    }

    private String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
