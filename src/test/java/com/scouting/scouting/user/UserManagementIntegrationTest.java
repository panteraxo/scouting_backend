package com.scouting.scouting.user;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.organization.repository.OrganizationCodeSequenceRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scouting_users;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "NuevaPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserOrganizationRepository userOrganizationRepository;

    @Autowired
    private AcademyRepository academyRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private OrganizationCodeSequenceRepository sequenceRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AtomicInteger sequence = new AtomicInteger(1);

    @BeforeEach
    void cleanDatabase() {
        userOrganizationRepository.deleteAll();
        academyRepository.deleteAll();
        clubRepository.deleteAll();
        userRepository.deleteAll();
        sequenceRepository.deleteAll();
    }

    // ---------------------------------------------------------------------
    // Matriz de creacion y contraseña temporal
    // ---------------------------------------------------------------------

    @Test
    void adminCreatesEveryRoleWithCorrectAssociationAndOneTimePassword() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-matrix@example.com");
        Academy academy = createAcademy("Academia Matriz", OperationalStatus.ACTIVE);
        Club club = createClub("Club Matriz", OperationalStatus.ACTIVE);
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<Role, Long> expectedAcademy = Map.of(
                Role.PROFESSOR, academy.getId(),
                Role.ACADEMY_DIRECTOR, academy.getId());
        Map<Role, Long> expectedClub = Map.of(Role.CLUB_MANAGER, club.getId());

        for (Role role : Role.values()) {
            if (role == Role.PLAYER) {
                Map<String, Object> playerBody = createBody(Role.PLAYER, "matrix-player@example.com");
                playerBody.put("academyId", academy.getId());
                assertCreateRejected(session, csrf, playerBody, 400, "INVALID_ROLE");
                continue;
            }
            String email = "matrix-" + role.name().toLowerCase() + "@example.com";
            Map<String, Object> body = createBody(role, email);
            if (expectedAcademy.containsKey(role)) {
                body.put("academyId", academy.getId());
            }
            if (expectedClub.containsKey(role)) {
                body.put("clubId", club.getId());
                body.put("position", "Coordinador");
            }

            MvcResult result = mockMvc.perform(post("/api/v1/admin/users")
                            .session(session)
                            .header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Cache-Control", containsString("no-store")))
                    .andExpect(jsonPath("$.user.role").value(role.name()))
                    .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                    .andExpect(jsonPath("$.temporaryPasswordExpiresAt").isNotEmpty())
                    .andExpect(jsonPath("$.user.documentNumber").doesNotExist())
                    .andExpect(jsonPath("$.user.phone").doesNotExist())
                    .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            String temporaryPassword = response.get("temporaryPassword").asText();
            User stored = userRepository.findByEmail(email).orElseThrow();
            assertEquals(role, stored.getRole());
            assertEquals(UserStatus.ACTIVE, stored.getStatus());
            assertTrue(stored.isMustChangePassword());
            assertNotNull(stored.getTemporaryPasswordExpiresAt());
            assertTrue(stored.getTemporaryPasswordExpiresAt().isAfter(Instant.now().plusSeconds(23 * 3600)));
            assertTrue(stored.getTemporaryPasswordExpiresAt().isBefore(Instant.now().plusSeconds(25 * 3600)));
            assertNotEquals(temporaryPassword, stored.getPasswordHash());
            assertTrue(passwordEncoder.matches(temporaryPassword, stored.getPasswordHash()));

            var association = userOrganizationRepository.findByUser_Id(stored.getId());
            if (expectedAcademy.containsKey(role)) {
                assertEquals(academy.getId(), association.orElseThrow().getAcademy().getId());
                assertNull(association.orElseThrow().getClub());
                assertNull(association.orElseThrow().getPosition());
            } else if (expectedClub.containsKey(role)) {
                assertEquals(club.getId(), association.orElseThrow().getClub().getId());
                assertNull(association.orElseThrow().getAcademy());
                assertEquals("Coordinador", association.orElseThrow().getPosition());
            } else {
                assertTrue(association.isEmpty());
            }
        }
    }

    @Test
    void massAssignmentFieldsAreIgnoredOnCreateAndIdentityUpdate() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-mass@example.com");
        Academy academy = createAcademy("Academia Mass", OperationalStatus.ACTIVE);
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> body = createBody(Role.SCOUT, "mass-scout@example.com");
        body.put("id", 9999);
        body.put("status", "INACTIVE");
        body.put("password", "InjectedPassword123!");
        body.put("passwordHash", "hash-inyectado");
        body.put("mustChangePassword", false);
        body.put("temporaryPasswordExpiresAt", null);
        body.put("tutorName", "Tutor Inyectado");
        body.put("privateNotes", "Nota privada");

        mockMvc.perform(post("/api/v1/admin/users")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("SCOUT"))
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.id").value(org.hamcrest.Matchers.not(9999)));

        User scout = userRepository.findByEmail("mass-scout@example.com").orElseThrow();
        assertNotEquals(9999L, scout.getId());
        assertEquals(UserStatus.ACTIVE, scout.getStatus());
        assertTrue(scout.isMustChangePassword());
        assertFalse(passwordEncoder.matches("InjectedPassword123!", scout.getPasswordHash()));

        Map<String, Object> playerBody = createBody(Role.PLAYER, "mass-player@example.com");
        playerBody.put("academyId", academy.getId());
        assertCreateRejected(session, csrf, playerBody, 400, "INVALID_ROLE");

        User player = createUser(Role.PLAYER, UserStatus.ACTIVE, "mass-player-direct@example.com");
        associate(player, academy, null);
        long playerId = player.getId();

        Map<String, Object> identity = identityBody(player);
        identity.put("role", "ADMIN");
        identity.put("status", "INACTIVE");
        identity.put("academyId", null);
        identity.put("password", "InjectedPassword123!");
        identity.put("id", 123456);

        mockMvc.perform(put("/api/v1/admin/users/" + playerId)
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(identity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.academyId").value(academy.getId()));

        User after = userRepository.findById(playerId).orElseThrow();
        assertEquals(Role.PLAYER, after.getRole());
        assertEquals(UserStatus.ACTIVE, after.getStatus());
        assertEquals(academy.getId(), userOrganizationRepository.findByUser_Id(playerId)
                .orElseThrow().getAcademy().getId());
    }

    @Test
    void scopedCreationUsesSessionAcademyAndEnforcesRoleMatrix() throws Exception {
        Academy own = createAcademy("Academia Propia", OperationalStatus.ACTIVE);
        Academy other = createAcademy("Academia Ajena", OperationalStatus.ACTIVE);
        User director = createUser(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-scope@example.com");
        associate(director, own, null);
        User professor = createUser(Role.PROFESSOR, UserStatus.ACTIVE, "professor-scope@example.com");
        associate(professor, own, null);
        User player = createUser(Role.PLAYER, UserStatus.ACTIVE, "player-scope@example.com");
        associate(player, own, null);

        MockHttpSession directorSession = login(director.getEmail());
        Csrf directorCsrf = csrf(directorSession);
        Map<String, Object> playerBody = createBody(Role.PLAYER, "director-player@example.com");
        playerBody.put("academyId", other.getId());
        playerBody.put("clubId", 9999);
        playerBody.put("status", "INACTIVE");

        mockMvc.perform(post("/api/v1/academies/me/users")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(playerBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_ALLOWED"));

        mockMvc.perform(post("/api/v1/academies/me/users")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody(Role.PROFESSOR, "director-professor@example.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PROFESSOR"))
                .andExpect(jsonPath("$.user.academyId").value(own.getId()));

        mockMvc.perform(post("/api/v1/academies/me/users")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody(Role.ADMIN, "director-admin@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_ALLOWED"));

        MockHttpSession professorSession = login(professor.getEmail());
        Csrf professorCsrf = csrf(professorSession);
        mockMvc.perform(post("/api/v1/academies/me/users")
                        .session(professorSession).header("X-CSRF-TOKEN", professorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody(Role.PLAYER, "professor-player@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_ALLOWED"));

        mockMvc.perform(post("/api/v1/academies/me/users")
                        .session(professorSession).header("X-CSRF-TOKEN", professorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody(Role.PROFESSOR, "professor-professor@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_ALLOWED"));

        for (Role denied : List.of(Role.PLAYER, Role.SCOUT, Role.CLUB_MANAGER)) {
            User deniedUser = createUser(denied, UserStatus.ACTIVE,
                    "denied-" + denied.name().toLowerCase() + "@example.com");
            MockHttpSession deniedSession = login(deniedUser.getEmail());
            Csrf deniedCsrf = csrf(deniedSession);
            mockMvc.perform(post("/api/v1/academies/me/users")
                            .session(deniedSession).header("X-CSRF-TOKEN", deniedCsrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(createBody(Role.PLAYER, "denied-create-" + denied.name() + "@example.com"))))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get("/api/v1/admin/users").session(professorSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        own.setOperationalStatus(OperationalStatus.INACTIVE);
        academyRepository.saveAndFlush(own);
        mockMvc.perform(post("/api/v1/academies/me/users")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody(Role.PROFESSOR, "inactive-academy-create@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_INACTIVE"));
    }

    @Test
    void documentIsMandatoryAndPhoneIsOptionalForInternalAccounts() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-optional-phone@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> missingDocument = createBody(Role.SCOUT, "missing-document@example.com");
        missingDocument.remove("documentNumber");
        mockMvc.perform(post("/api/v1/admin/users")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(missingDocument)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.documentNumber").exists());

        Map<String, Object> withoutPhone = createBody(Role.SCOUT, "without-phone@example.com");
        withoutPhone.remove("phone");
        mockMvc.perform(post("/api/v1/admin/users")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(withoutPhone)))
                .andExpect(status().isCreated());
        assertNull(userRepository.findByEmail("without-phone@example.com").orElseThrow().getPhone());

        Map<String, Object> blankPhone = createBody(Role.SCOUT, "blank-phone@example.com");
        blankPhone.put("phone", "");
        mockMvc.perform(post("/api/v1/admin/users")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(blankPhone)))
                .andExpect(status().isCreated());
        assertNull(userRepository.findByEmail("blank-phone@example.com").orElseThrow().getPhone());
    }

    // ---------------------------------------------------------------------
    // Organizacion, asignaciones y duplicados
    // ---------------------------------------------------------------------

    @Test
    void adminCreationAndAssignmentRejectInvalidOrInactiveOrganizations() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-org@example.com");
        Academy activeAcademy = createAcademy("Academia Activa", OperationalStatus.ACTIVE);
        Academy inactiveAcademy = createAcademy("Academia Inactiva", OperationalStatus.INACTIVE);
        Club activeClub = createClub("Club Activo", OperationalStatus.ACTIVE);
        Club inactiveClub = createClub("Club Inactivo", OperationalStatus.INACTIVE);
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        assertCreateRejected(session, csrf, createBody(Role.PROFESSOR, "no-academy@example.com"),
                400, "INVALID_ORGANIZATION_ASSIGNMENT");
        Map<String, Object> professorClub = createBody(Role.PROFESSOR, "professor-club@example.com");
        professorClub.put("clubId", activeClub.getId());
        assertCreateRejected(session, csrf, professorClub, 400, "INVALID_ORGANIZATION_ASSIGNMENT");

        Map<String, Object> playerClub = createBody(Role.PLAYER, "player-club@example.com");
        playerClub.put("clubId", activeClub.getId());
        assertCreateRejected(session, csrf, playerClub, 400, "INVALID_ROLE");

        Map<String, Object> managerAcademy = createBody(Role.CLUB_MANAGER, "manager-academy@example.com");
        managerAcademy.put("academyId", activeAcademy.getId());
        assertCreateRejected(session, csrf, managerAcademy, 400, "INVALID_ORGANIZATION_ASSIGNMENT");

        Map<String, Object> adminAcademy = createBody(Role.ADMIN, "admin-academy@example.com");
        adminAcademy.put("academyId", activeAcademy.getId());
        assertCreateRejected(session, csrf, adminAcademy, 400, "INVALID_ORGANIZATION_ASSIGNMENT");

        Map<String, Object> inactiveClubBody = createBody(Role.CLUB_MANAGER, "inactive-club@example.com");
        inactiveClubBody.put("clubId", inactiveClub.getId());
        assertCreateRejected(session, csrf, inactiveClubBody, 409, "ORGANIZATION_INACTIVE");

        Map<String, Object> inactiveAcademyBody = createBody(Role.PROFESSOR, "inactive-academy@example.com");
        inactiveAcademyBody.put("academyId", inactiveAcademy.getId());
        assertCreateRejected(session, csrf, inactiveAcademyBody, 409, "ORGANIZATION_INACTIVE");

        Map<String, Object> missingAcademyBody = createBody(Role.PROFESSOR, "missing-academy@example.com");
        missingAcademyBody.put("academyId", 999999);
        assertCreateRejected(session, csrf, missingAcademyBody, 404, "ORGANIZATION_NOT_FOUND");

        User assignedPlayer = createUser(Role.PLAYER, UserStatus.ACTIVE, "assigned-player@example.com");
        associate(assignedPlayer, activeAcademy, null);
        long playerId = assignedPlayer.getId();

        mockMvc.perform(patch("/api/v1/admin/users/" + playerId + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.PLAYER, inactiveAcademy.getId(), null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_INACTIVE"));

        mockMvc.perform(patch("/api/v1/admin/users/" + playerId + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.PLAYER, 999999L, null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

        mockMvc.perform(patch("/api/v1/admin/users/" + playerId + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.PLAYER, activeAcademy.getId(), null, "Cargo"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORGANIZATION_ASSIGNMENT"));

        mockMvc.perform(patch("/api/v1/admin/users/" + playerId + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.CLUB_MANAGER, null, activeClub.getId(), "Director deportivo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLUB_MANAGER"))
                .andExpect(jsonPath("$.academyId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.clubId").value(activeClub.getId()))
                .andExpect(jsonPath("$.position").value("Director deportivo"));

        UserOrganization clubAssociation = userOrganizationRepository.findByUser_Id(playerId).orElseThrow();
        assertNull(clubAssociation.getAcademy());
        assertEquals(activeClub.getId(), clubAssociation.getClub().getId());
        assertEquals("Director deportivo", clubAssociation.getPosition());

        mockMvc.perform(patch("/api/v1/admin/users/" + playerId + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.SCOUT, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SCOUT"))
                .andExpect(jsonPath("$.academyId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.clubId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.position").value(org.hamcrest.Matchers.nullValue()));
        assertTrue(userOrganizationRepository.findByUser_Id(playerId).isEmpty());

        mockMvc.perform(patch("/api/v1/admin/users/" + playerId + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.PLAYER, activeAcademy.getId(), null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.academyId").value(activeAcademy.getId()));
        assertEquals(activeAcademy.getId(), userOrganizationRepository.findByUser_Id(playerId)
                .orElseThrow().getAcademy().getId());
    }

    @Test
    void duplicateIdentityAcrossEmailUsernameOrDocumentReturnsGenericConflict() throws Exception {
        User existing = createUser(Role.SCOUT, UserStatus.ACTIVE, "duplicate@example.com");
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-duplicate@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        List<String> messages = new ArrayList<>();

        Map<String, Object> duplicateEmail = createBody(Role.SCOUT, existing.getEmail());
        messages.add(assertCreateRejected(session, csrf, duplicateEmail, 409, "USER_ALREADY_EXISTS"));

        Map<String, Object> duplicateUsername = createBody(Role.SCOUT, "different-email@example.com");
        duplicateUsername.put("username", existing.getUsername());
        messages.add(assertCreateRejected(session, csrf, duplicateUsername, 409, "USER_ALREADY_EXISTS"));

        Map<String, Object> duplicateDocument = createBody(Role.SCOUT, "different-doc@example.com");
        duplicateDocument.put("documentNumber", existing.getDocumentNumber());
        messages.add(assertCreateRejected(session, csrf, duplicateDocument, 409, "USER_ALREADY_EXISTS"));

        messages.forEach(message -> assertEquals(messages.get(0), message));

        User second = createUser(Role.SCOUT, UserStatus.ACTIVE, "second-duplicate@example.com");
        Map<String, Object> update = identityBody(second);
        update.put("email", existing.getEmail());
        mockMvc.perform(put("/api/v1/admin/users/" + second.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value(messages.get(0)));
    }

    // ---------------------------------------------------------------------
    // Contraseñas temporales, reset y estados
    // ---------------------------------------------------------------------

    @Test
    void temporaryPasswordFlowRequiresChangeAndNeverLeaksSecretsInReads() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-temp@example.com");
        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);

        Map<String, Object> body = createBody(Role.SCOUT, "temporary@example.com");
        MvcResult created = mockMvc.perform(post("/api/v1/admin/users")
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andReturn();
        JsonNode createdNode = objectMapper.readTree(created.getResponse().getContentAsString());
        long userId = createdNode.get("user").get("id").asLong();
        String temporaryPassword = createdNode.get("temporaryPassword").asText();

        String detail = mockMvc.perform(get("/api/v1/admin/users/" + userId).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value(body.get("documentNumber")))
                .andExpect(jsonPath("$.phone").value(body.get("phone")))
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        assertFalse(detail.toLowerCase().contains("tutor"));
        assertFalse(detail.toLowerCase().contains("nota"));

        String list = mockMvc.perform(get("/api/v1/admin/users").session(adminSession))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertFalse(list.contains("documentNumber"));
        assertFalse(list.contains("phone"));
        assertFalse(list.contains("passwordHash"));
        assertFalse(list.contains(temporaryPassword));
        assertFalse(list.toLowerCase().contains("tutor"));
        assertFalse(list.toLowerCase().contains("nota"));

        MockHttpSession temporarySession = login("temporary@example.com", temporaryPassword);
        mockMvc.perform(get("/api/v1/auth/me").session(temporarySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
        mockMvc.perform(get("/api/v1/directory/academies").session(temporarySession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        Csrf temporaryCsrf = csrf(temporarySession);
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .session(temporarySession).header("X-CSRF-TOKEN", temporaryCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", temporaryPassword, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        loginExpectingFailure("temporary@example.com", temporaryPassword);
        MockHttpSession changedSession = login("temporary@example.com", NEW_PASSWORD);
        mockMvc.perform(get("/api/v1/auth/me").session(changedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));
    }

    @Test
    void adminResetOnlyWorksForActiveAccountsAndReturnsOneTimeSecret() throws Exception {
        User active = createUser(Role.SCOUT, UserStatus.ACTIVE, "reset-active@example.com");
        User inactive = createUser(Role.SCOUT, UserStatus.INACTIVE, "reset-inactive@example.com");
        User pending = createUser(Role.SCOUT, UserStatus.PENDING_APPROVAL, "reset-pending@example.com");
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-reset@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        MvcResult reset = mockMvc.perform(post("/api/v1/admin/users/" + active.getId() + "/reset-password")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andReturn();
        String temporaryPassword = objectMapper.readTree(reset.getResponse().getContentAsString())
                .get("temporaryPassword").asText();

        User stored = userRepository.findById(active.getId()).orElseThrow();
        assertTrue(stored.isMustChangePassword());
        assertNotNull(stored.getTemporaryPasswordExpiresAt());
        assertTrue(passwordEncoder.matches(temporaryPassword, stored.getPasswordHash()));
        loginExpectingFailure(active.getEmail(), PASSWORD);
        MockHttpSession temporarySession = login(active.getEmail(), temporaryPassword);
        mockMvc.perform(get("/api/v1/auth/me").session(temporarySession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));

        mockMvc.perform(post("/api/v1/admin/users/" + inactive.getId() + "/reset-password")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));

        mockMvc.perform(post("/api/v1/admin/users/" + pending.getId() + "/reset-password")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDING_ACCOUNT_NOT_MANAGED"));
    }

    @Test
    void statusChangesInvalidateSessionsAndPendingAccountsUseReviewFlowOnly() throws Exception {
        User scout = createUser(Role.SCOUT, UserStatus.ACTIVE, "status-scout@example.com");
        User pending = createUser(Role.SCOUT, UserStatus.PENDING_APPROVAL, "status-pending@example.com");
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-status@example.com");
        MockHttpSession scoutSession = login(scout.getEmail());
        mockMvc.perform(get("/api/v1/auth/me").session(scoutSession)).andExpect(status().isOk());

        MockHttpSession adminSession = login(admin.getEmail());
        Csrf csrf = csrf(adminSession);

        mockMvc.perform(patch("/api/v1/admin/users/" + scout.getId() + "/status")
                        .session(adminSession).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/auth/me").session(scoutSession))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
        loginExpectingFailure(scout.getEmail(), PASSWORD);

        mockMvc.perform(patch("/api/v1/admin/users/" + scout.getId() + "/status")
                        .session(adminSession).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());
        login(scout.getEmail(), PASSWORD);

        mockMvc.perform(patch("/api/v1/admin/users/" + pending.getId() + "/status")
                        .session(adminSession).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDING_ACCOUNT_NOT_MANAGED"));

        mockMvc.perform(patch("/api/v1/admin/users/" + scout.getId() + "/status")
                        .session(adminSession).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PENDING_APPROVAL"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));

        mockMvc.perform(patch("/api/v1/admin/users/" + pending.getId() + "/assignment")
                        .session(adminSession).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.SCOUT, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDING_ACCOUNT_NOT_MANAGED"));

        mockMvc.perform(put("/api/v1/admin/users/" + pending.getId())
                        .session(adminSession).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(identityBody(pending))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PENDING_ACCOUNT_NOT_MANAGED"));

        mockMvc.perform(delete("/api/v1/admin/users/" + scout.getId())
                        .session(adminSession).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isMethodNotAllowed());
        assertTrue(userRepository.findById(scout.getId()).isPresent());
    }

    @Test
    void selfDowngradeSelfInactivationAndLastAdminAreProtected() throws Exception {
        User firstAdmin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-first@example.com");
        MockHttpSession firstSession = login(firstAdmin.getEmail());
        Csrf firstCsrf = csrf(firstSession);

        mockMvc.perform(patch("/api/v1/admin/users/" + firstAdmin.getId() + "/status")
                        .session(firstSession).header("X-CSRF-TOKEN", firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INACTIVE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_DEACTIVATION_NOT_ALLOWED"));

        mockMvc.perform(patch("/api/v1/admin/users/" + firstAdmin.getId() + "/assignment")
                        .session(firstSession).header("X-CSRF-TOKEN", firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.SCOUT, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE_NOT_ALLOWED"));

        User secondAdmin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-second@example.com");
        mockMvc.perform(patch("/api/v1/admin/users/" + secondAdmin.getId() + "/assignment")
                        .session(firstSession).header("X-CSRF-TOKEN", firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.SCOUT, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SCOUT"));

        mockMvc.perform(patch("/api/v1/admin/users/" + firstAdmin.getId() + "/assignment")
                        .session(firstSession).header("X-CSRF-TOKEN", firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.SCOUT, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE_NOT_ALLOWED"));

        mockMvc.perform(patch("/api/v1/admin/users/" + secondAdmin.getId() + "/assignment")
                        .session(firstSession).header("X-CSRF-TOKEN", firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.ADMIN, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        mockMvc.perform(patch("/api/v1/admin/users/" + secondAdmin.getId() + "/status")
                        .session(firstSession).header("X-CSRF-TOKEN", firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/v1/admin/users/" + firstAdmin.getId() + "/assignment")
                        .session(firstSession).header("X-CSRF-TOKEN", firstCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody(Role.SCOUT, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE_NOT_ALLOWED"));
    }

    // ---------------------------------------------------------------------
    // Listado, CSRF y regresiones de permisos
    // ---------------------------------------------------------------------

    @Test
    void adminListSupportsFiltersPaginationSortAndEscapedLiteralSearch() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-list@example.com");
        Academy academyA = createAcademy("Academia Lista A", OperationalStatus.ACTIVE);
        Academy academyB = createAcademy("Academia Lista B", OperationalStatus.ACTIVE);
        Club club = createClub("Club Lista", OperationalStatus.ACTIVE);

        User percent = createUser(Role.PLAYER, UserStatus.ACTIVE, "percent@example.com");
        percent.setFirstName("Cien %");
        userRepository.saveAndFlush(percent);
        associate(percent, academyA, null);

        User underscore = createUser(Role.PLAYER, UserStatus.ACTIVE, "underscore@example.com");
        underscore.setFirstName("Guion_Bajo");
        userRepository.saveAndFlush(underscore);
        associate(underscore, academyA, null);

        User exclamation = createUser(Role.PLAYER, UserStatus.ACTIVE, "exclamation@example.com");
        exclamation.setFirstName("Exclamacion!");
        userRepository.saveAndFlush(exclamation);
        associate(exclamation, academyB, null);

        User manager = createUser(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-list@example.com");
        associate(manager, club, "Coordinador");

        User inactive = createUser(Role.SCOUT, UserStatus.INACTIVE, "inactive-list@example.com");
        User pending = createUser(Role.SCOUT, UserStatus.PENDING_APPROVAL, "pending-list@example.com");
        MockHttpSession session = login(admin.getEmail());

        mockMvc.perform(get("/api/v1/admin/users").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.content[0].documentNumber").doesNotExist())
                .andExpect(jsonPath("$.content[0].phone").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/users").session(session).param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5));
        mockMvc.perform(get("/api/v1/admin/users").session(session).param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(inactive.getId()));

        mockMvc.perform(get("/api/v1/admin/users").session(session).param("role", "PLAYER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get("/api/v1/admin/users").session(session)
                        .param("academyId", academyA.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/admin/users").session(session)
                        .param("clubId", club.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(manager.getId()));

        mockMvc.perform(get("/api/v1/admin/users").session(session)
                        .param("academyId", academyA.getId().toString())
                        .param("clubId", club.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER_COMBINATION"));
        mockMvc.perform(get("/api/v1/admin/users").session(session)
                        .param("status", "PENDING_APPROVAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_FILTER"));

        mockMvc.perform(get("/api/v1/admin/users").session(session).param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(percent.getId()));
        mockMvc.perform(get("/api/v1/admin/users").session(session).param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(underscore.getId()));
        mockMvc.perform(get("/api/v1/admin/users").session(session).param("q", "!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(exclamation.getId()));

        mockMvc.perform(get("/api/v1/admin/users").session(session)
                        .param("sort", "passwordHash,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
        mockMvc.perform(get("/api/v1/admin/users").session(session)
                        .param("size", "2")
                        .param("sort", "lastName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(3));

        assertTrue(userRepository.findById(pending.getId()).isPresent());
    }

    @Test
    void writesRequireCsrfAndNonAdminRolesCannotUseUserModule() throws Exception {
        User admin = createUser(Role.ADMIN, UserStatus.ACTIVE, "admin-csrf-users@example.com");
        User scout = createUser(Role.SCOUT, UserStatus.ACTIVE, "scout-csrf-users@example.com");
        MockHttpSession adminSession = login(admin.getEmail());

        mockMvc.perform(post("/api/v1/admin/users")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(createBody(Role.SCOUT, "without-csrf@example.com"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        MockHttpSession scoutSession = login(scout.getEmail());
        mockMvc.perform(get("/api/v1/admin/users").session(scoutSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/admin/users/" + admin.getId()).session(scoutSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/academies/me/users").session(scoutSession))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private String assertCreateRejected(MockHttpSession session, Csrf csrf, Map<String, Object> body,
                                        int expectedStatus, String expectedCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/users")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("message").asText();
    }

    private User createUser(Role role, UserStatus status, String email) {
        int n = sequence.incrementAndGet();
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setUsername("user" + n);
        user.setFirstName("Nombre" + n);
        user.setLastName("Apellido" + n);
        user.setDocumentType(DocumentType.DNI);
        user.setDocumentNumber("DOC" + n);
        user.setPhone("999999999");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(status);
        user.setMustChangePassword(false);
        user.setTemporaryPasswordExpiresAt(null);
        return userRepository.saveAndFlush(user);
    }

    private Academy createAcademy(String name, OperationalStatus status) {
        int n = sequence.incrementAndGet();
        Academy academy = new Academy();
        academy.setCode("ACA-USR-" + n);
        academy.setName(name);
        academy.setLogoUrl("https://cdn.example.com/academy.png");
        academy.setType("Academia de futbol");
        academy.setDescription("Descripcion institucional.");
        academy.setFoundationYear(2001);
        academy.setRuc(String.format("20%09d", n));
        academy.setUbigeoCode("150101");
        academy.setAddress("Av. Institucional 123");
        academy.setInstitutionalEmail("contacto@academia.example.com");
        academy.setInstitutionalPhone("+51 1 555 0101");
        academy.setHeadquarters("Sede principal");
        academy.setRepresentativeName("Ana Torres");
        academy.setOperationalStatus(status);
        academy.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        return academyRepository.saveAndFlush(academy);
    }

    private Club createClub(String name, OperationalStatus status) {
        int n = sequence.incrementAndGet();
        Club club = new Club();
        club.setCode("CLU-USR-" + n);
        club.setName(name);
        club.setType("Club de futbol");
        club.setRuc(String.format("20%09d", n));
        club.setFoundationYear(1998);
        club.setLogoUrl("https://cdn.example.com/club.png");
        club.setDescription("Descripcion institucional del club.");
        club.setCountryCode("PE");
        club.setUbigeoCode("150101");
        club.setAddress("Av. Institucional 456");
        club.setInstitutionalEmail("contacto@club.example.com");
        club.setInstitutionalPhone("+51 1 555 0202");
        club.setOperationalStatus(status);
        club.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
        return clubRepository.saveAndFlush(club);
    }

    private UserOrganization associate(User user, Academy academy, String position) {
        return saveAssociation(user, academy, null, position);
    }

    private UserOrganization associate(User user, Club club, String position) {
        return saveAssociation(user, null, club, position);
    }

    private UserOrganization saveAssociation(User user, Academy academy, Club club, String position) {
        UserOrganization association = new UserOrganization();
        association.setUser(user);
        association.setAcademy(academy);
        association.setClub(club);
        association.setPosition(position);
        return userOrganizationRepository.saveAndFlush(association);
    }

    private Map<String, Object> createBody(Role role, String email) {
        int n = sequence.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", "Nombre" + n);
        body.put("lastName", "Apellido" + n);
        body.put("documentType", "DNI");
        body.put("documentNumber", "CRT" + n);
        body.put("phone", "999888777");
        body.put("email", email);
        body.put("username", "created" + n);
        body.put("role", role.name());
        return body;
    }

    private Map<String, Object> identityBody(User user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", user.getFirstName());
        body.put("lastName", user.getLastName());
        body.put("documentType", user.getDocumentType().name());
        body.put("documentNumber", user.getDocumentNumber());
        body.put("phone", user.getPhone());
        body.put("email", user.getEmail());
        body.put("username", user.getUsername());
        return body;
    }

    private Map<String, Object> assignmentBody(Role role, Long academyId, Long clubId, String position) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", role.name());
        body.put("academyId", academyId);
        body.put("clubId", clubId);
        body.put("position", position);
        return body;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private MockHttpSession login(String email) throws Exception {
        return login(email, PASSWORD);
    }

    private MockHttpSession login(String email, String password) throws Exception {
        Csrf csrf = obtainCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private void loginExpectingFailure(String email, String password) throws Exception {
        Csrf csrf = obtainCsrf();
        mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales invalidas."));
    }

    private Csrf obtainCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(node.get("token").asText(), (MockHttpSession) result.getRequest().getSession(false));
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(node.get("token").asText(), session);
    }

    private record Csrf(String token, MockHttpSession session) {
    }
}
