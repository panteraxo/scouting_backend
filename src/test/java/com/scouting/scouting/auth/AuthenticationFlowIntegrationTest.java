package com.scouting.scouting.auth;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scouting_flow;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${server.servlet.session.cookie.http-only:false}")
    private boolean sessionCookieHttpOnly;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void sessionCookieIsConfiguredHttpOnly() {
        assertTrue(sessionCookieHttpOnly);
    }

    @Test
    void csrfEndpointIsPublicAndReturnsHeaderName() throws Exception {
        Csrf csrf = obtainCsrf();
        assertNotNull(csrf.token());
        assertNotNull(csrf.session());
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(post("/api/v1/auth/register/scout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationBody("dup@example.com", "dupuser", "11111111"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void wrongCsrfTokenIsReportedAsCsrfInvalid() throws Exception {
        Csrf csrf = obtainCsrf();

        mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", "token-incorrecto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nadie@example.com\",\"password\":\"Password123!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    @Test
    void openEndpointsNotDeclaredAreDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/scout-registrations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scoutRegistrationForcesRoleAndStatusAndDoesNotCreateSession() throws Exception {
        Map<String, Object> body = registrationBody("scout@example.com", "scoutuser", "12345678");
        body.put("role", "ADMIN");
        body.put("status", "ACTIVE");
        body.put("organizationId", 1);
        body.put("organization", "Club Pirata");

        Csrf csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/register/scout")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").isNotEmpty());

        User stored = userRepository.findByEmail("scout@example.com").orElseThrow();
        assertEquals(Role.SCOUT, stored.getRole());
        assertEquals(UserStatus.PENDING_APPROVAL, stored.getStatus());
        assertFalse(stored.isMustChangePassword());
        assertEquals(null, stored.getTemporaryPasswordExpiresAt());
        assertNotEquals("Password123!", stored.getPasswordHash());
        assertTrue(passwordEncoder.matches("Password123!", stored.getPasswordHash()));

        // El registro publico no inicia sesion.
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void scoutRegistrationWithoutPhoneCreatesPendingApprovalAccount() throws Exception {
        Map<String, Object> body = registrationBody("nophone@example.com", "nophoneuser", "10000001");
        body.remove("phone");

        Csrf csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/register/scout")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message")
                        .value("Solicitud registrada. Un administrador revisara la cuenta para habilitarla."));

        User stored = userRepository.findByEmail("nophone@example.com").orElseThrow();
        assertEquals(Role.SCOUT, stored.getRole());
        assertEquals(UserStatus.PENDING_APPROVAL, stored.getStatus());
        assertNull(stored.getPhone());
        assertFalse(stored.isMustChangePassword());
        assertTrue(passwordEncoder.matches("Password123!", stored.getPasswordHash()));
    }

    @Test
    void scoutRegistrationWithValidPhoneStoresIt() throws Exception {
        Map<String, Object> body = registrationBody("phone@example.com", "phoneuser", "10000002");
        body.put("phone", "+51 987 654 321");

        Csrf csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/register/scout")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted());

        User stored = userRepository.findByEmail("phone@example.com").orElseThrow();
        assertEquals(Role.SCOUT, stored.getRole());
        assertEquals(UserStatus.PENDING_APPROVAL, stored.getStatus());
        assertEquals("+51 987 654 321", stored.getPhone());
    }

    @Test
    void scoutRegistrationWithBlankPhoneStoresNull() throws Exception {
        Map<String, Object> body = registrationBody("blankphone@example.com", "blankphoneuser", "10000003");
        body.put("phone", "");

        Csrf csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/register/scout")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted());

        User stored = userRepository.findByEmail("blankphone@example.com").orElseThrow();
        assertEquals(Role.SCOUT, stored.getRole());
        assertEquals(UserStatus.PENDING_APPROVAL, stored.getStatus());
        assertNull(stored.getPhone());
    }

    @Test
    void scoutRegistrationWithInvalidPhoneReturnsValidationErrorAndDoesNotCreateUser() throws Exception {
        Map<String, Object> body = registrationBody("invalidphone@example.com", "invalidphoneuser", "10000004");
        body.put("phone", "abc");

        Csrf csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/register/scout")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertEquals(0, userRepository.count());
        assertFalse(userRepository.findByEmail("invalidphone@example.com").isPresent());
    }

    @Test
    void duplicateScoutRegistrationReturnsSameGenericResponseWithoutRevealingExistence() throws Exception {
        String firstBody = registerScout("dup@example.com", "dupuser", "87654321");
        assertEquals(1, userRepository.count());

        String secondBody = registerScout("dup@example.com", "otrosuser", "99999999");
        assertEquals(1, userRepository.count());
        assertEquals(firstBody, secondBody);

        String thirdBody = registerScout("otro@example.com", "dupuser", "55555555");
        assertEquals(1, userRepository.count());
        assertEquals(firstBody, thirdBody);
    }

    @Test
    void pendingScoutCannotLogin() throws Exception {
        registerScout("pending@example.com", "pendinguser", "22222222");
        loginExpectingFailure("pending@example.com", "Password123!");
    }

    @Test
    void inactiveUserCannotLogin() throws Exception {
        createUser(Role.SCOUT, UserStatus.INACTIVE, "inactive@example.com", "Password123!", false, null);
        loginExpectingFailure("inactive@example.com", "Password123!");
    }

    @Test
    void unknownUserAndWrongPasswordReturnSameGenericMessage() throws Exception {
        createUser(Role.SCOUT, UserStatus.ACTIVE, "real@example.com", "Password123!", false, null);

        String unknownMessage = loginFailureMessage("nadie@example.com", "Password123!");
        String wrongPasswordMessage = loginFailureMessage("real@example.com", "OtraPassword123!");

        assertEquals(unknownMessage, wrongPasswordMessage);
        assertEquals("Credenciales invalidas.", unknownMessage);
    }

    @Test
    void loginCreatesSessionChangesSessionIdAndMeDoesNotExposeSecrets() throws Exception {
        createUser(Role.ADMIN, UserStatus.ACTIVE, "admin@example.com", "AdminPassword123!", false, null);

        Csrf csrf = obtainCsrf();
        String oracleSessionId = csrf.session().getId();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@example.com", "AdminPassword123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.mustChangePassword").value(false))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);
        assertNotEquals(oracleSessionId, session.getId());

        String meBody = mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andReturn().getResponse().getContentAsString();
        assertFalse(meBody.contains("passwordHash"));
        assertFalse(meBody.contains("password"));
        assertFalse(meBody.toLowerCase().contains("tutor"));
        assertFalse(meBody.toLowerCase().contains("nota"));

        Csrf logoutCsrf = obtainCsrf(session);
        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .header("X-CSRF-TOKEN", logoutCsrf.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onlyAdminCanListApproveAndRejectScoutRegistrations() throws Exception {
        registerScout("approve@example.com", "approveuser", "33333333");
        registerScout("reject@example.com", "rejectuser", "44444444");
        createUser(Role.ADMIN, UserStatus.ACTIVE, "boss@example.com", "BossPassword123!", false, null);

        MockHttpSession adminSession = login("boss@example.com", "BossPassword123!");
        Csrf adminCsrf = obtainCsrf(adminSession);

        mockMvc.perform(get("/api/v1/admin/scout-registrations")
                        .session(adminSession)
                        .param("status", "PENDING_APPROVAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist());

        Long approveId = userRepository.findByEmail("approve@example.com").orElseThrow().getId();
        Long rejectId = userRepository.findByEmail("reject@example.com").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/admin/scout-registrations/" + approveId + "/approve")
                        .session(adminSession)
                        .header("X-CSRF-TOKEN", adminCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/scout-registrations/" + rejectId + "/reject")
                        .session(adminSession)
                        .header("X-CSRF-TOKEN", adminCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        MockHttpSession approvedSession = login("approve@example.com", "Password123!");
        mockMvc.perform(get("/api/v1/auth/me").session(approvedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SCOUT"));

        loginExpectingFailure("reject@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/admin/scout-registrations").session(approvedSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        Csrf scoutCsrf = obtainCsrf(approvedSession);
        mockMvc.perform(post("/api/v1/admin/scout-registrations/" + rejectId + "/approve")
                        .session(approvedSession)
                        .header("X-CSRF-TOKEN", scoutCsrf.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void temporaryPasswordOnlyAllowsPasswordChange() throws Exception {
        createUser(Role.ADMIN, UserStatus.ACTIVE, "temp@example.com", "TempPassword123!", true,
                Instant.now().plusSeconds(3600));

        MockHttpSession session = login("temp@example.com", "TempPassword123!");

        mockMvc.perform(get("/api/v1/admin/scout-registrations").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("TempPassword123!", "NuevaPassword123!")))
                .andExpect(status().isForbidden());

        Csrf csrf = obtainCsrf(session);
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("Incorrecta123!", "NuevaPassword123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("TempPassword123!", "TempPassword123!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_UNCHANGED"));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePasswordBody("TempPassword123!", "NuevaPassword123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        mockMvc.perform(get("/api/v1/admin/scout-registrations").session(session))
                .andExpect(status().isOk());

        loginExpectingFailure("temp@example.com", "TempPassword123!");
        login("temp@example.com", "NuevaPassword123!");
    }

    @Test
    void expiredTemporaryPasswordCannotLogin() throws Exception {
        createUser(Role.ADMIN, UserStatus.ACTIVE, "expired@example.com", "ExpiredPassword123!", true,
                Instant.now().minusSeconds(60));
        loginExpectingFailure("expired@example.com", "ExpiredPassword123!");
    }

    @Test
    void statusChangeInvalidatesExistingSession() throws Exception {
        User scout = createUser(Role.SCOUT, UserStatus.ACTIVE, "revoked@example.com", "Password123!", false, null);
        MockHttpSession session = login("revoked@example.com", "Password123!");

        mockMvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());

        scout.setStatus(UserStatus.INACTIVE);
        userRepository.saveAndFlush(scout);

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidationRejectsMissingFields() throws Exception {
        Csrf csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/register/scout")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-valido\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        assertEquals(0, userRepository.count());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private Csrf obtainCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        return new Csrf(node.get("token").asText(), session);
    }

    private Csrf obtainCsrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(node.get("token").asText(), session);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        Csrf csrf = obtainCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void loginExpectingFailure(String email, String password) throws Exception {
        assertEquals("Credenciales invalidas.", loginFailureMessage(email, password));
    }

    private String loginFailureMessage(String email, String password) throws Exception {
        Csrf csrf = obtainCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, password)))
                .andExpect(status().isUnauthorized())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("message").asText();
    }

    private String registerScout(String email, String username, String documentNumber) throws Exception {
        Csrf csrf = obtainCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register/scout")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                registrationBody(email, username, documentNumber))))
                .andExpect(status().isAccepted())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private User createUser(Role role, UserStatus status, String email, String rawPassword,
                            boolean mustChangePassword, Instant temporaryPasswordExpiresAt) {
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setUsername(email.substring(0, email.indexOf('@')).toLowerCase());
        user.setFirstName("Nombre");
        user.setLastName("Apellido");
        user.setDocumentType(DocumentType.DNI);
        user.setDocumentNumber("DOC-" + Math.abs(email.hashCode()));
        user.setPhone("999999999");
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(status);
        user.setMustChangePassword(mustChangePassword);
        user.setTemporaryPasswordExpiresAt(temporaryPasswordExpiresAt);
        return userRepository.saveAndFlush(user);
    }

    private Map<String, Object> registrationBody(String email, String username, String documentNumber) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", "Juan");
        body.put("lastName", "Perez");
        body.put("documentType", "DNI");
        body.put("documentNumber", documentNumber);
        body.put("email", email);
        body.put("phone", "987654321");
        body.put("username", username);
        body.put("password", "Password123!");
        return body;
    }

    private String loginBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    private String changePasswordBody(String currentPassword, String newPassword) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("currentPassword", currentPassword, "newPassword", newPassword));
    }

    private record Csrf(String token, MockHttpSession session) {
    }
}
