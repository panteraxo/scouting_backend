package com.scouting.scouting.prospecting;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.player.service.PlayerTime;
import com.scouting.scouting.prospecting.repository.ProspectEvaluationRepository;
import com.scouting.scouting.prospecting.repository.ProspectRepository;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        "spring.datasource.url=jdbc:h2:mem:scouting_prospects;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProspectApiIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String LIMA = "150101";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AcademyRepository academyRepository;
    @Autowired
    private UserOrganizationRepository userOrganizationRepository;
    @Autowired
    private PlayerProfileRepository playerProfileRepository;
    @Autowired
    private ProspectRepository prospectRepository;
    @Autowired
    private ProspectEvaluationRepository evaluationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AtomicInteger sequence = new AtomicInteger(1);

    @BeforeEach
    void cleanDatabase() {
        evaluationRepository.deleteAll();
        prospectRepository.deleteAll();
        playerProfileRepository.deleteAll();
        userOrganizationRepository.deleteAll();
        academyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void scoutCreatesReadsAndUpdatesOwnProspect() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-prospect@example.com");
        Academy academy = academy("Academia Prospecto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-prospect@example.com");
        createProfile(player, admin);

        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        MvcResult created = mockMvc.perform(post("/api/v1/prospects")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("playerId", profileId(player), "privateNotes", "Nota inicial"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value("FOLLOWING"))
                .andExpect(jsonPath("$.privateNotes").value("Nota inicial"))
                .andExpect(jsonPath("$.player.id").value(profileId(player)))
                .andExpect(jsonPath("$.player.photoUrl").value("https://example.com/player.jpg"))
                .andExpect(jsonPath("$.player.birthDate").doesNotExist())
                .andExpect(jsonPath("$.player.residence").doesNotExist())
                .andExpect(jsonPath("$.player.academy.verificationStatus").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.scoutId").doesNotExist())
                .andReturn();
        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        long prospectId = createdJson.get("id").asLong();
        long version = createdJson.get("version").asLong();
        assertNotNull(createdJson.get("startedAt").asText());
        assertNotNull(createdJson.get("lastActivityAt").asText());

        mockMvc.perform(get("/api/v1/prospects").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(prospectId))
                .andExpect(jsonPath("$.content[0].player.photoUrl").value("https://example.com/player.jpg"))
                .andExpect(jsonPath("$.content[0].player.birthDate").doesNotExist())
                .andExpect(jsonPath("$.content[0].player.residence").doesNotExist());
        mockMvc.perform(get("/api/v1/prospects/" + prospectId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateNotes").value("Nota inicial"))
                .andExpect(jsonPath("$.player.photoUrl").value("https://example.com/player.jpg"));

        MvcResult notes = mockMvc.perform(patch("/api/v1/prospects/" + prospectId)
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("privateNotes", "Nota actualizada", "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.privateNotes").value("Nota actualizada"))
                .andReturn();
        version = objectMapper.readTree(notes.getResponse().getContentAsString()).get("version").asLong();

        version = patchStatus(session, csrf, prospectId, "PAUSED", version);
        version = patchStatus(session, csrf, prospectId, "FOLLOWING", version);
        MvcResult closed = mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "CLOSED", "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closedAt").isNotEmpty())
                .andReturn();
        version = objectMapper.readTree(closed.getResponse().getContentAsString()).get("version").asLong();

        mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "FOLLOWING", "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FOLLOWING"))
                .andExpect(jsonPath("$.closedAt").value(nullValue()));

        mockMvc.perform(delete("/api/v1/prospects/" + prospectId)
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void ownershipAndRoleMatrix() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-matrix-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-matrix-prospect@example.com");
        User otherScout = user(Role.SCOUT, UserStatus.ACTIVE, "other-scout-matrix@example.com");
        User professor = user(Role.PROFESSOR, UserStatus.ACTIVE, "professor-matrix-prospect@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-matrix-prospect@example.com");
        User club = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "club-matrix-prospect@example.com");
        User playerUser = user(Role.PLAYER, UserStatus.ACTIVE, "player-matrix-prospect@example.com");
        Academy academy = academy("Academia Matrix Prospecto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(professor, academy);
        associate(director, academy);
        User player = playerIn(academy, "player-target-matrix@example.com");
        createProfile(player, admin);

        MockHttpSession scoutSession = login(scout.getEmail());
        Csrf scoutCsrf = csrf(scoutSession);
        long prospectId = createProspect(scoutSession, scoutCsrf, player.getId());

        MockHttpSession otherSession = login(otherScout.getEmail());
        mockMvc.perform(get("/api/v1/prospects/" + prospectId).session(otherSession))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROSPECT_NOT_FOUND"));
        mockMvc.perform(patch("/api/v1/prospects/" + prospectId)
                        .session(otherSession).header("X-CSRF-TOKEN", csrf(otherSession).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("privateNotes", "No permitido", "version", 0))))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/prospects/" + prospectId + "/evaluations").session(otherSession))
                .andExpect(status().isNotFound());

        for (User denied : List.of(admin, professor, director, club, playerUser)) {
            mockMvc.perform(get("/api/v1/prospects").session(login(denied.getEmail())))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            mockMvc.perform(get("/api/v1/prospects/" + prospectId).session(login(denied.getEmail())))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void createRejectsInexistentIncompleteInactiveAccountAndInactiveAcademyPlayers() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-ineligible-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-ineligible-prospect@example.com");
        Academy activeAcademy = academy("Academia Ineligible Active", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Academy inactiveAcademy = academy("Academia Ineligible Inactive", OperationalStatus.INACTIVE,
                VerificationStatus.PENDING_VERIFICATION);

        User incompletePlayer = playerIn(activeAcademy, "player-incomplete-prospect@example.com");
        PlayerProfile incompleteProfile = createProfile(incompletePlayer, admin);
        incompleteProfile.setProfileStatus(PlayerProfileStatus.INCOMPLETE);
        playerProfileRepository.saveAndFlush(incompleteProfile);

        User inactiveAccount = playerIn(activeAcademy, "player-inactive-account-prospect@example.com");
        createProfile(inactiveAccount, admin);
        inactiveAccount.setStatus(UserStatus.INACTIVE);
        userRepository.saveAndFlush(inactiveAccount);

        User inactiveAcademyPlayer = playerIn(inactiveAcademy, "player-inactive-academy-prospect@example.com");
        createProfile(inactiveAcademyPlayer, admin);

        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        for (long playerId : List.of(999999L, profileId(incompletePlayer),
                profileId(inactiveAccount), profileId(inactiveAcademyPlayer))) {
            mockMvc.perform(post("/api/v1/prospects")
                            .session(session).header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("playerId", playerId))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLAYER_NOT_FOUND"));
        }
    }

    @Test
    void evaluationFromAnotherProspectIsNotFound() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-eval-other-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-eval-other-prospect@example.com");
        Academy academy = academy("Academia Eval Other", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player1 = playerIn(academy, "player-eval-other-one@example.com");
        User player2 = playerIn(academy, "player-eval-other-two@example.com");
        createProfile(player1, admin);
        createProfile(player2, admin);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        long prospect1 = createProspect(session, csrf, player1.getId());
        long prospect2 = createProspect(session, csrf, player2.getId());

        MvcResult evaluation = mockMvc.perform(post("/api/v1/prospects/" + prospect1 + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody(null))))
                .andExpect(status().isCreated())
                .andReturn();
        long evaluationId = objectMapper.readTree(evaluation.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/v1/prospects/" + prospect2 + "/evaluations/" + evaluationId)
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROSPECT_EVALUATION_NOT_FOUND"));
    }

    @Test
    void oneProspectPerScoutAndPlayerButSeveralScoutsCanFollow() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-unique-prospect@example.com");
        User scoutA = user(Role.SCOUT, UserStatus.ACTIVE, "scout-a-unique@example.com");
        User scoutB = user(Role.SCOUT, UserStatus.ACTIVE, "scout-b-unique@example.com");
        Academy academy = academy("Academia Unique Prospecto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-unique-prospect@example.com");
        createProfile(player, admin);

        MockHttpSession sessionA = login(scoutA.getEmail());
        Csrf csrfA = csrf(sessionA);
        createProspect(sessionA, csrfA, player.getId());
        mockMvc.perform(post("/api/v1/prospects")
                        .session(sessionA).header("X-CSRF-TOKEN", csrfA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("playerId", profileId(player)))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROSPECT_ALREADY_EXISTS"));

        MockHttpSession sessionB = login(scoutB.getEmail());
        createProspect(sessionB, csrf(sessionB), player.getId());

        assertEquals(2, prospectRepository.count());
    }

    @Test
    void statusTransitionsAreValidatedAndVersionIsRequired() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-status-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-status-prospect@example.com");
        Academy academy = academy("Academia Status Prospecto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-status-prospect@example.com");
        createProfile(player, admin);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        long prospectId = createProspect(session, csrf, player.getId());

        mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "FOLLOWING", "version", 0))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PAUSED", "version", 99))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_PROSPECT"));

        long closedVersion = patchStatus(session, csrf, prospectId, "CLOSED", 0);
        mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PAUSED", "version", closedVersion))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));
    }

    @Test
    void evaluationsAreAppendOnlyImmutableAndDoNotExposePii() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-eval-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-eval-prospect@example.com");
        Academy academy = academy("Academia Eval Prospecto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-eval-prospect@example.com");
        createProfile(player, admin);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        long prospectId = createProspect(session, csrf, player.getId());

        Map<String, Object> firstBody = evaluationBody(null);
        firstBody.put("scoutId", 99999);
        firstBody.put("createdBy", 99999);
        firstBody.put("createdAt", "2000-01-01T00:00:00Z");
        MvcResult first = mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(firstBody)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.technicalRating").value(4))
                .andExpect(jsonPath("$.author").doesNotExist())
                .andExpect(jsonPath("$.birthDate").doesNotExist())
                .andReturn();
        long firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asLong();

        MvcResult second = mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody("Segunda evaluacion"))))
                .andExpect(status().isCreated())
                .andReturn();
        long secondId = objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asLong();
        assertNotEquals(firstId, secondId);
        assertEquals(2, evaluationRepository.count());

        mockMvc.perform(get("/api/v1/prospects/" + prospectId + "/evaluations").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/prospects/" + prospectId + "/evaluations/" + firstId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prospectId").value(prospectId));
        mockMvc.perform(put("/api/v1/prospects/" + prospectId + "/evaluations/" + firstId)
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody("Intento de edicion"))))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/prospects/" + prospectId + "/evaluations/" + firstId)
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void evaluationUpdatesProspectActivityAndVersion() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-eval-version@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-eval-version@example.com");
        Academy academy = academy("Academia Eval Version", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-eval-version@example.com");
        createProfile(player, admin);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        long prospectId = createProspect(session, csrf, player.getId());

        MvcResult before = mockMvc.perform(get("/api/v1/prospects/" + prospectId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode beforeJson = objectMapper.readTree(before.getResponse().getContentAsString());
        long oldVersion = beforeJson.get("version").asLong();
        String oldActivity = beforeJson.get("lastActivityAt").asText();

        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody(null))))
                .andExpect(status().isCreated());

        MvcResult after = mockMvc.perform(get("/api/v1/prospects/" + prospectId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode afterJson = objectMapper.readTree(after.getResponse().getContentAsString());
        long newVersion = afterJson.get("version").asLong();
        assertTrue(newVersion > oldVersion);
        assertTrue(java.time.Instant.parse(afterJson.get("lastActivityAt").asText())
                .isAfter(java.time.Instant.parse(oldActivity)));

        mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PAUSED", "version", oldVersion))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_PROSPECT"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "PAUSED", "version", newVersion))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test
    void evaluationValidationOwnershipClosedProspectAndPlayerEligibility() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-eval-validation@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-eval-validation@example.com");
        User otherScout = user(Role.SCOUT, UserStatus.ACTIVE, "other-eval-validation@example.com");
        Academy academy = academy("Academia Eval Validation", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-eval-validation@example.com");
        createProfile(player, admin);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        long prospectId = createProspect(session, csrf, player.getId());

        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("evaluatedAt", PlayerTime.today().toString(),
                                "technicalRating", 0, "tacticalRating", 4, "physicalRating", 4, "mentalRating", 4))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Map<String, Object> future = evaluationBody(null);
        future.put("evaluatedAt", PlayerTime.today().plusDays(1).toString());
        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(future)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EVALUATION_DATE"));

        Map<String, Object> beforeBirth = evaluationBody(null);
        beforeBirth.put("evaluatedAt", PlayerTime.today().minusYears(30).toString());
        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(beforeBirth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EVALUATION_DATE"));

        Map<String, Object> badUrl = evaluationBody(null);
        badUrl.put("videoUrl", "not-a-url");
        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        MockHttpSession otherSession = login(otherScout.getEmail());
        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(otherSession).header("X-CSRF-TOKEN", csrf(otherSession).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody(null))))
                .andExpect(status().isNotFound());

        long version = currentVersion(session, prospectId);
        patchStatus(session, csrf, prospectId, "CLOSED", version);
        mockMvc.perform(post("/api/v1/prospects/" + prospectId + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody(null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PROSPECT_CLOSED"));

        User player2 = playerIn(academy, "player-eval-inactive@example.com");
        createProfile(player2, admin);
        long prospect2 = createProspect(session, csrf, player2.getId());
        player2.setStatus(UserStatus.INACTIVE);
        userRepository.saveAndFlush(player2);
        mockMvc.perform(post("/api/v1/prospects/" + prospect2 + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody(null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));

        User player3 = playerIn(academy, "player-eval-profile-inactive@example.com");
        PlayerProfile profile3 = createProfile(player3, admin);
        long prospect3 = createProspect(session, csrf, player3.getId());
        profile3.setOperationalStatus(PlayerOperationalStatus.INACTIVE);
        playerProfileRepository.saveAndFlush(profile3);
        mockMvc.perform(post("/api/v1/prospects/" + prospect3 + "/evaluations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(evaluationBody(null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));
    }

    @Test
    void filtersPaginationSortAndLiteralSearch() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-filters-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-filters-prospect@example.com");
        Academy academy = academy("Academia Filters Prospecto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player1 = playerIn(academy, "player-one-filters@example.com");
        player1.setFirstName("Cien %");
        player1.setLastName("Zulu");
        userRepository.saveAndFlush(player1);
        createProfile(player1, admin);
        User player2 = playerIn(academy, "player-two-filters@example.com");
        player2.setFirstName("Guion_Bajo");
        player2.setLastName("Alfa");
        userRepository.saveAndFlush(player2);
        createProfile(player2, admin);
        User player3 = playerIn(academy, "player-three-filters@example.com");
        player3.setFirstName("Exclamacion!");
        player3.setLastName("Mana");
        userRepository.saveAndFlush(player3);
        createProfile(player3, admin);

        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        long firstProspect = createProspect(session, csrf, player1.getId());
        createProspect(session, csrf, player2.getId());
        createProspect(session, csrf, player3.getId());
        long version = currentVersion(session, firstProspect);
        patchStatus(session, csrf, firstProspect, "PAUSED", version);

        mockMvc.perform(get("/api/v1/prospects").session(session).param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].player.id").value(profileId(player1)));
        mockMvc.perform(get("/api/v1/prospects").session(session).param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].player.id").value(profileId(player2)));
        mockMvc.perform(get("/api/v1/prospects").session(session).param("q", "!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].player.id").value(profileId(player3)));
        mockMvc.perform(get("/api/v1/prospects").session(session).param("status", "PAUSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/prospects").session(session)
                        .param("playerId", profileId(player2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/prospects").session(session)
                        .param("page", "0").param("size", "1").param("sort", "playerName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].player.id").value(profileId(player2)));
        mockMvc.perform(get("/api/v1/prospects").session(session).param("sort", "privateNotes,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));
    }

    @Test
    void csrfAndNoStoreAreEnforced() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-csrf-prospect@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-csrf-prospect@example.com");
        Academy academy = academy("Academia CSRF Prospecto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-csrf-prospect@example.com");
        createProfile(player, admin);
        MockHttpSession session = login(scout.getEmail());

        mockMvc.perform(post("/api/v1/prospects")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("playerId", profileId(player)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(get("/api/v1/prospects").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(patch("/api/v1/prospects/1")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("privateNotes", "sin csrf", "version", 0))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(delete("/api/v1/prospects/1").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(get("/api/v1/prospects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private long createProspect(MockHttpSession session, Csrf csrf, Long userId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/prospects")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("playerId", profileId(userId)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Long profileId(User user) {
        return playerProfileRepository.findByUser_Id(user.getId())
                .orElseThrow().getId();
    }

    private Long profileId(Long userId) {
        return playerProfileRepository.findByUser_Id(userId).orElseThrow().getId();
    }

    private long patchStatus(MockHttpSession session, Csrf csrf, long prospectId,
                             String status, long version) throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/v1/prospects/" + prospectId + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", status, "version", version))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("version").asLong();
    }

    private long currentVersion(MockHttpSession session, long prospectId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/prospects/" + prospectId).session(session))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("version").asLong();
    }

    private Map<String, Object> evaluationBody(String privateNotes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("evaluatedAt", PlayerTime.today().toString());
        body.put("technicalRating", 4);
        body.put("tacticalRating", 4);
        body.put("physicalRating", 4);
        body.put("mentalRating", 4);
        body.put("overallRating", 4);
        body.put("strengths", "Buena lectura");
        body.put("improvements", "Mejorar definicion");
        body.put("recommendation", "Seguir");
        if (privateNotes != null) {
            body.put("privateNotes", privateNotes);
        }
        body.put("videoUrl", "https://example.com/video");
        return body;
    }

    private User user(Role role, UserStatus status, String email) {
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

    private User playerIn(Academy academy, String email) {
        User player = user(Role.PLAYER, UserStatus.ACTIVE, email);
        associate(player, academy);
        return player;
    }

    private Academy academy(String name, OperationalStatus operational, VerificationStatus verification) {
        int n = sequence.incrementAndGet();
        Academy academy = new Academy();
        academy.setCode("ACA-PRS-" + n);
        academy.setName(name);
        academy.setLogoUrl("https://cdn.example.com/academy.png");
        academy.setType("Academia de futbol");
        academy.setDescription("Descripcion institucional.");
        academy.setFoundationYear(2001);
        academy.setRuc(String.format("20%09d", n));
        academy.setUbigeoCode(LIMA);
        academy.setAddress("Av. Institucional 123");
        academy.setInstitutionalEmail("contacto@academia.example.com");
        academy.setInstitutionalPhone("+51 1 555 0101");
        academy.setHeadquarters("Sede principal");
        academy.setRepresentativeName("Ana Torres");
        academy.setOperationalStatus(operational);
        academy.setVerificationStatus(verification);
        return academyRepository.saveAndFlush(academy);
    }

    private UserOrganization associate(User user, Academy academy) {
        UserOrganization association = new UserOrganization();
        association.setUser(user);
        association.setAcademy(academy);
        association.setClub(null);
        association.setPosition(null);
        return userOrganizationRepository.saveAndFlush(association);
    }

    private PlayerProfile createProfile(User user, User operator) {
        PlayerProfile profile = new PlayerProfile();
        profile.setUser(user);
        profile.setBirthDate(PlayerTime.today().minusYears(20));
        profile.setResidenceUbigeoCode(LIMA);
        profile.setHeightCm(175);
        profile.setWeightKg(new BigDecimal("70.00"));
        profile.setPosition(PlayerPosition.STRIKER);
        profile.setBestPosition(PlayerPosition.STRIKER);
        profile.setDominantFoot(DominantFoot.RIGHT);
        profile.setSummary("Resumen");
        profile.setPhotoUrl("https://example.com/player.jpg");
        profile.setOperationalStatus(PlayerOperationalStatus.ACTIVE);
        profile.setProfileStatus(PlayerProfileStatus.COMPLETE);
        profile.setCompletedAt(Instant.now());
        profile.setCreatedBy(operator);
        profile.setUpdatedBy(operator);
        return playerProfileRepository.saveAndFlush(profile);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private MockHttpSession login(String email) throws Exception {
        Csrf csrf = obtainCsrf();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .session(csrf.session())
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private Csrf csrf(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(node.get("token").asText(), session);
    }

    private Csrf obtainCsrf() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Csrf(node.get("token").asText(), (MockHttpSession) result.getRequest().getSession(false));
    }

    private record Csrf(String token, MockHttpSession session) {
    }
}
