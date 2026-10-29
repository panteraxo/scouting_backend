package com.scouting.scouting.player;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.AgeCategory;
import com.scouting.scouting.player.entity.CompetitiveLevel;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.GuardianConsentStatus;
import com.scouting.scouting.player.entity.Participation;
import com.scouting.scouting.player.entity.PlayerGuardian;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.entity.TeamResult;
import com.scouting.scouting.player.repository.PlayerGuardianConsentEventRepository;
import com.scouting.scouting.player.repository.PlayerGuardianRepository;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.player.repository.PlayerHistoryMetricRepository;
import com.scouting.scouting.player.repository.PlayerHistoryRevisionRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.player.service.PlayerTime;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
        "spring.datasource.url=jdbc:h2:mem:scouting_players;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlayerApiIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String LIMA = "150101";
    private static final String AREQUIPA = "040101";

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
    private PlayerGuardianRepository guardianRepository;
    @Autowired
    private PlayerGuardianConsentEventRepository consentRepository;
    @Autowired
    private PlayerHistoryEntryRepository historyRepository;
    @Autowired
    private PlayerHistoryMetricRepository metricRepository;
    @Autowired
    private PlayerHistoryRevisionRepository revisionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final AtomicInteger sequence = new AtomicInteger(1);

    @BeforeEach
    void cleanDatabase() {
        revisionRepository.deleteAll();
        metricRepository.deleteAll();
        historyRepository.deleteAll();
        consentRepository.deleteAll();
        guardianRepository.deleteAll();
        playerProfileRepository.deleteAll();
        userOrganizationRepository.deleteAll();
        academyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCreatesMinorIncompleteAndStaffRegistersGuardian() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-minor@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-minor@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-minor@example.com");
        User player = user(Role.PLAYER, UserStatus.ACTIVE, "player-minor@example.com");
        User manager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-minor@example.com");
        Academy academy = academy("Academia Menor", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, academy);
        associate(player, academy);

        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);
        MvcResult created = mockMvc.perform(post("/api/v1/players")
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileBody(player.getId(), true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.yearsExperience").value(nullValue()))
                .andExpect(jsonPath("$.guardianRequired").value(true))
                .andExpect(jsonPath("$.guardian").doesNotExist())
                .andExpect(jsonPath("$.guardianDataStatus").doesNotExist())
                .andReturn();
        long playerId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/players/" + playerId + "/guardian").session(adminSession))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GUARDIAN_ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/players/" + playerId).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardian").doesNotExist())
                .andExpect(jsonPath("$.guardianDataStatus").doesNotExist());

        MockHttpSession directorSession = login(director.getEmail());
        Csrf directorCsrf = csrf(directorSession);
        mockMvc.perform(put("/api/v1/players/" + playerId + "/guardian")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(guardianBody(null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Maria"))
                .andExpect(jsonPath("$.consent.status").value("NOT_RECORDED"))
                .andExpect(jsonPath("$.email").doesNotExist());

        mockMvc.perform(get("/api/v1/players/" + playerId).session(directorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.guardianDataStatus").value("RECORDED"));

        MockHttpSession scoutSession = login(scout.getEmail());
        mockMvc.perform(get("/api/v1/players/" + playerId).session(scoutSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthDate").doesNotExist())
                .andExpect(jsonPath("$.residence.districtCode").doesNotExist())
                .andExpect(jsonPath("$.residence.districtName").doesNotExist())
                .andExpect(jsonPath("$.academy.verificationStatus").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.guardianDataAvailable").value(true));
        mockMvc.perform(get("/api/v1/players/" + playerId + "/guardian").session(scoutSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.consent.recordedBy").doesNotExist())
                .andExpect(jsonPath("$.consent.evidenceUrl").doesNotExist())
                .andExpect(jsonPath("$.consent.note").doesNotExist());

        MockHttpSession playerSession = login(player.getEmail());
        mockMvc.perform(get("/api/v1/players/me").session(playerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardian").doesNotExist())
                .andExpect(jsonPath("$.canEdit").value(false));
        mockMvc.perform(put("/api/v1/players/" + playerId)
                        .session(playerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileBody(player.getId(), false))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/players/" + playerId + "/guardian").session(playerSession))
                .andExpect(status().isForbidden());

        MockHttpSession managerSession = login(manager.getEmail());
        mockMvc.perform(get("/api/v1/players").session(managerSession))
                .andExpect(status().isForbidden());

        assertTrue(guardianRepository.existsByPlayerProfile_Id(playerId));
    }

    @Test
    void scoutOnlySeesActiveCompleteProfilesFromActiveAcademiesAndSeesVerification() throws Exception {
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-visibility@example.com");
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-visibility@example.com");

        Academy pending = academy("Academia Pendiente", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Academy rejected = academy("Academia Rechazada", OperationalStatus.ACTIVE, VerificationStatus.REJECTED);
        Academy inactive = academy("Academia Inactiva", OperationalStatus.INACTIVE, VerificationStatus.VERIFIED);
        User completePending = playerIn(pending, "complete-pending@example.com");
        User completeRejected = playerIn(rejected, "complete-rejected@example.com");
        User incomplete = playerIn(pending, "incomplete@example.com");
        User inInactive = playerIn(inactive, "inactive-academy@example.com");

        createProfile(completePending, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);
        createProfile(completeRejected, admin, true, PlayerTime.today().minusYears(17), AREQUIPA, PlayerPosition.CENTER_BACK);
        createProfile(incomplete, admin, false, PlayerTime.today().minusYears(15), LIMA, PlayerPosition.LEFT_WINGER);
        createProfile(inInactive, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);

        MockHttpSession scoutSession = login(scout.getEmail());
        mockMvc.perform(get("/api/v1/players").session(scoutSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.content[0].residence.districtName").doesNotExist())
                .andExpect(jsonPath("$.content[0].operationalStatus").doesNotExist())
                .andExpect(jsonPath("$.content[0].profileStatus").doesNotExist())
                .andExpect(jsonPath("$.content[0].academy.verificationStatus").isString());
        mockMvc.perform(get("/api/v1/players").session(scoutSession)
                        .param("operationalStatus", "INACTIVE"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/players").session(scoutSession)
                        .param("districtCode", LIMA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/players").session(scoutSession)
                        .param("sort", "birthDate,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));

        mockMvc.perform(get("/api/v1/players").session(login(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].residence.districtCode").isString())
                .andExpect(jsonPath("$.content[0].residence.districtName").isString());
    }

    @Test
    void directoryIncludesHeightAndWeightForStaffAndScout() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-directory-measures@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-directory-measures@example.com");
        Academy academy = academy("Academia Medidas", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-directory-measures@example.com");
        createProfile(player, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);

        mockMvc.perform(get("/api/v1/players").session(login(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].heightCm").value(170))
                .andExpect(jsonPath("$.content[0].weightKg").value(65.0))
                .andExpect(jsonPath("$.content[0].birthDate").doesNotExist());

        mockMvc.perform(get("/api/v1/players").session(login(scout.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].heightCm").value(170))
                .andExpect(jsonPath("$.content[0].weightKg").value(65.0))
                .andExpect(jsonPath("$.content[0].birthDate").doesNotExist())
                .andExpect(jsonPath("$.content[0].residence.districtName").doesNotExist());
    }

    @Test
    void guardianConsentIsAuditedAndRevocableWithoutLeakingEvidenceToScout() throws Exception {
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-consent@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-consent@example.com");
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-consent@example.com");
        Academy academy = academy("Academia Consentimiento", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, academy);
        User player = playerIn(academy, "player-consent@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);
        createGuardian(profile, director);

        MockHttpSession session = login(director.getEmail());
        Csrf csrf = csrf(session);
        mockMvc.perform(put("/api/v1/players/" + profile.getId() + "/guardian/consent")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "GRANTED", "evidenceUrl", "https://example.com/consent",
                                "note", "Firmado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consent.status").value("GRANTED"))
                .andExpect(jsonPath("$.consent.recordedBy.id").value(director.getId()));
        mockMvc.perform(put("/api/v1/players/" + profile.getId() + "/guardian/consent")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "REVOKED", "note", "Revocado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consent.status").value("REVOKED"));

        assertEquals(2, consentRepository.findByPlayerProfile_IdOrderByRecordedAtDescIdDesc(profile.getId()).size());

        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/guardian").session(login(scout.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consent.status").value("REVOKED"))
                .andExpect(jsonPath("$.consent.evidenceUrl").doesNotExist())
                .andExpect(jsonPath("$.consent.note").doesNotExist())
                .andExpect(jsonPath("$.consent.recordedBy").doesNotExist());
        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/guardian").session(login(admin.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GUARDIAN_ACCESS_DENIED"));
    }

    @Test
    void historyIsAppendOnlyAndCorrectionsUseCurrentAcademy() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-history@example.com");
        User directorA = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-a@example.com");
        User directorB = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-b@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-history@example.com");
        Academy academyA = academy("Academia Historial A", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Academy academyB = academy("Academia Historial B", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(directorA, academyA);
        associate(directorB, academyB);
        User player = playerIn(academyA, "player-history@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);

        MockHttpSession sessionA = login(directorA.getEmail());
        Csrf csrfA = csrf(sessionA);
        MvcResult first = mockMvc.perform(post("/api/v1/players/" + profile.getId() + "/history")
                        .session(sessionA).header("X-CSRF-TOKEN", csrfA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(historyBody(ActivityType.OFFICIAL_MATCH, "2", "1", "WIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.revisionNumber").value(1))
                .andReturn();
        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());
        long historyId = firstJson.get("id").asLong();
        long expectedVersion = firstJson.get("version").asLong();

        mockMvc.perform(post("/api/v1/players/" + profile.getId() + "/history")
                        .session(sessionA).header("X-CSRF-TOKEN", csrfA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(historyBody(ActivityType.FRIENDLY_MATCH, "1", "1", "DRAW"))))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/history").session(sessionA))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        Map<String, Object> correction = historyBody(ActivityType.OFFICIAL_MATCH, "3", "1", "WIN");
        correction.put("expectedVersion", expectedVersion);
        correction.put("correctionReason", "Se corrigio el marcador.");
        MvcResult corrected = mockMvc.perform(put("/api/v1/players/" + profile.getId() + "/history/" + historyId)
                        .session(sessionA).header("X-CSRF-TOKEN", csrfA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(correction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(2))
                .andExpect(jsonPath("$.author.id").value(directorA.getId()))
                .andReturn();
        long versionAfterCorrection = objectMapper.readTree(corrected.getResponse().getContentAsString())
                .get("version").asLong();

        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/history/" + historyId + "/revisions")
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(delete("/api/v1/players/" + profile.getId() + "/history/" + historyId)
                        .session(sessionA).header("X-CSRF-TOKEN", csrfA.token()))
                .andExpect(status().isMethodNotAllowed());

        // El jugador cambia de academia: la academia anterior pierde el alcance.
        userOrganizationRepository.findByUser_Id(player.getId()).ifPresent(userOrganizationRepository::delete);
        userOrganizationRepository.flush();
        associate(player, academyB);

        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/history").session(sessionA))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/players/" + profile.getId() + "/history/" + historyId)
                        .session(sessionA).header("X-CSRF-TOKEN", csrfA.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(correction)))
                .andExpect(status().isNotFound());

        MockHttpSession sessionB = login(directorB.getEmail());
        Csrf csrfB = csrf(sessionB);
        Map<String, Object> correctionB = historyBody(ActivityType.OFFICIAL_MATCH, "2", "0", "WIN");
        correctionB.put("expectedVersion", versionAfterCorrection);
        correctionB.put("correctionReason", "Correccion de la academia actual.");
        mockMvc.perform(put("/api/v1/players/" + profile.getId() + "/history/" + historyId)
                        .session(sessionB).header("X-CSRF-TOKEN", csrfB.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(correctionB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revisionNumber").value(3))
                .andExpect(jsonPath("$.author.id").value(directorA.getId()));

        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/history/" + historyId + "/revisions")
                        .session(login(scout.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void historyRejectsResultAndMetricInconsistencies() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-validation@example.com");
        Academy academy = academy("Academia Validacion", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-validation@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> validMetrics = historyBody(ActivityType.OFFICIAL_MATCH, "1", "0", "WIN");
        validMetrics.put("metrics", Map.of("forward", Map.of(
                "goals", 1, "assists", 1, "shots", 3, "successfulDribbles", 2,
                "chancesCreated", 2, "crossesAttempted", 4, "crossesCompleted", 2)));
        mockMvc.perform(post("/api/v1/players/" + profile.getId() + "/history")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validMetrics)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metrics.forward.shots").value(3))
                .andExpect(jsonPath("$.metrics.goalkeeper").value(nullValue()));

        Map<String, Object> mismatch = historyBody(ActivityType.OFFICIAL_MATCH, "2", "1", "LOSS");
        postHistory(session, csrf, profile, mismatch, 400, "RESULT_SCORE_MISMATCH");

        Map<String, Object> trainingResult = historyBody(ActivityType.TRAINING, null, null, "WIN");
        postHistory(session, csrf, profile, trainingResult, 400, "RESULT_NOT_APPLICABLE");

        Map<String, Object> wrongMetrics = historyBody(ActivityType.OFFICIAL_MATCH, "1", "0", "WIN");
        wrongMetrics.put("metrics", Map.of("goalkeeper", Map.of("saves", 3)));
        postHistory(session, csrf, profile, wrongMetrics, 400, "INVALID_POSITION_METRICS");

        Map<String, Object> badPasses = historyBody(ActivityType.OFFICIAL_MATCH, "1", "0", "WIN");
        badPasses.put("positionPlayed", "CENTRAL_MIDFIELDER");
        badPasses.put("metrics", Map.of("midfielder", Map.of("passesAttempted", 10, "passesCompleted", 11)));
        postHistory(session, csrf, profile, badPasses, 400, "METRIC_STAT_MISMATCH");

        Map<String, Object> withoutResult = historyBody(ActivityType.OFFICIAL_MATCH, "1", "0", "WIN");
        withoutResult.remove("teamResult");
        postHistory(session, csrf, profile, withoutResult, 400, "TEAM_RESULT_REQUIRED");

        Map<String, Object> extraMetric = historyBody(ActivityType.OFFICIAL_MATCH, "1", "0", "WIN");
        extraMetric.put("metrics", Map.of("forward", Map.of("goals", 1, "assists", 1, "unexpectedMetric", 9)));
        postHistory(session, csrf, profile, extraMetric, 400, "INVALID_REQUEST");

        Map<String, Object> negativeMetric = historyBody(ActivityType.OFFICIAL_MATCH, "1", "0", "WIN");
        negativeMetric.put("metrics", Map.of("forward", Map.of("shots", -1)));
        postHistory(session, csrf, profile, negativeMetric, 400, "VALIDATION_ERROR");

        Map<String, Object> didNotPlayWithMetrics = historyBody(ActivityType.OFFICIAL_MATCH, "1", "0", "WIN");
        didNotPlayWithMetrics.put("participation", "DID_NOT_PLAY");
        didNotPlayWithMetrics.put("minutesPlayed", null);
        didNotPlayWithMetrics.put("playerGoals", null);
        didNotPlayWithMetrics.put("assists", null);
        didNotPlayWithMetrics.put("metrics", Map.of("forward", Map.of("shots", 0)));
        postHistory(session, csrf, profile, didNotPlayWithMetrics, 400, "INVALID_HISTORY_STAT");
    }

    @Test
    void statsPreserveNullsCoverageAndActivityTypeSeparation() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-stats@example.com");
        Academy academy = academy("Academia Stats", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-stats@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);

        createHistory(profile, admin, ActivityType.TRAINING, PlayerTime.today().minusYears(5).minusDays(1),
                null, null, null);
        createHistory(profile, admin, ActivityType.OFFICIAL_MATCH, PlayerTime.today().minusMonths(2),
                Participation.STARTER, TeamResult.WIN, 90);
        createHistory(profile, admin, ActivityType.OFFICIAL_MATCH, PlayerTime.today().minusMonths(1),
                Participation.SUBSTITUTE, TeamResult.DRAW, null);
        createHistory(profile, admin, ActivityType.SPORTS_TRIAL, PlayerTime.today().minusDays(5),
                null, null, 30);

        MockHttpSession session = login(admin.getEmail());
        mockMvc.perform(get("/api/v1/players/" + profile.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.yearsExperience").value(5));
        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/stats").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.career.matchesRecorded").value(2))
                .andExpect(jsonPath("$.career.matchesPlayed.value").value(2))
                .andExpect(jsonPath("$.career.minutes.value").value(90))
                .andExpect(jsonPath("$.career.minutes.knownEntries").value(1))
                .andExpect(jsonPath("$.career.minutes.complete").value(false))
                .andExpect(jsonPath("$.career.goals.value").value(1))
                .andExpect(jsonPath("$.career.goals.knownEntries").value(1))
                .andExpect(jsonPath("$.career.goalsPerMatch.value").value(nullValue()))
                .andExpect(jsonPath("$.career.results.wins.value").value(1))
                .andExpect(jsonPath("$.career.results.draws.value").value(1))
                .andExpect(jsonPath("$.byActivityType[3].activityType").value("TRAINING"))
                .andExpect(jsonPath("$.byActivityType[3].totals.matchesPlayed.value").value(nullValue()))
                .andExpect(jsonPath("$.byActivityType[4].activityType").value("SPORTS_TRIAL"))
                .andExpect(jsonPath("$.byActivityType[4].totals.results").value(nullValue()));
    }

    @Test
    void filtersUseResidenceNotAcademyAndEscapeSearch() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-filters@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-filters@example.com");
        Academy limaAcademy = academy("Academia Lima", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(limaAcademy, "player-filters@example.com");
        player.setFirstName("Cien %");
        userRepository.saveAndFlush(player);
        createProfile(player, admin, true, PlayerTime.today().minusYears(16), AREQUIPA, PlayerPosition.STRIKER);

        mockMvc.perform(get("/api/v1/players").session(login(admin.getEmail()))
                        .param("regionCode", "04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/players").session(login(admin.getEmail()))
                        .param("regionCode", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/players").session(login(admin.getEmail()))
                        .param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/players").session(login(admin.getEmail()))
                        .param("positionGroup", "FORWARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/players").session(login(scout.getEmail()))
                        .param("districtCode", AREQUIPA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/players").session(login(admin.getEmail()))
                        .param("ageMin", "20")
                        .param("ageMax", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGE_RANGE"));
    }

    @Test
    void accountCandidatesAndListsRespectAcademyScope() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-candidates@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-candidates@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-candidates@example.com");
        Academy academyA = academy("Academia Candidatos A", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Academy academyB = academy("Academia Candidatos B", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, academyA);
        User candidateA = playerIn(academyA, "candidate-a@example.com");
        playerIn(academyA, "candidate-a-profiled@example.com");
        createProfile(userRepository.findByEmail("candidate-a-profiled@example.com").orElseThrow(),
                admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);
        User candidateB = playerIn(academyB, "candidate-b@example.com");

        MockHttpSession directorSession = login(director.getEmail());
        mockMvc.perform(get("/api/v1/players/account-candidates").session(directorSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(candidateA.getId()));
        mockMvc.perform(get("/api/v1/players").session(directorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].residence.districtCode").value(LIMA));
        mockMvc.perform(get("/api/v1/players").session(directorSession)
                        .param("academyId", academyB.getId().toString()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/players/account-candidates")
                        .session(login(admin.getEmail()))
                        .param("academyId", academyB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(candidateB.getId()));

        mockMvc.perform(get("/api/v1/players/account-candidates").session(login(scout.getEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCannotEnumerateAccountsOutsideTheirAcademyOnCreate() throws Exception {
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-scope-create@example.com");
        Academy ownAcademy = academy("Academia Scope Propia", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Academy otherAcademy = academy("Academia Scope Ajena", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, ownAcademy);

        User inactiveOutside = user(Role.PLAYER, UserStatus.INACTIVE, "inactive-outside@example.com");
        associate(inactiveOutside, otherAcademy);
        User nonPlayerOutside = user(Role.PROFESSOR, UserStatus.ACTIVE, "non-player-outside@example.com");
        associate(nonPlayerOutside, otherAcademy);
        User withoutAcademy = user(Role.PLAYER, UserStatus.ACTIVE, "without-academy@example.com");

        MockHttpSession session = login(director.getEmail());
        Csrf csrf = csrf(session);
        for (User outside : java.util.List.of(inactiveOutside, nonPlayerOutside, withoutAcademy)) {
            mockMvc.perform(post("/api/v1/players")
                            .session(session).header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(profileBody(outside.getId(), true))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PLAYER_ACCOUNT_NOT_FOUND"));
        }
    }

    @Test
    void birthDateCannotBeAfterTheEarliestHistoryActivity() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-birth-history@example.com");
        Academy academy = academy("Academia Birth History", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-birth-history@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(20), LIMA, PlayerPosition.STRIKER);
        createHistory(profile, admin, ActivityType.TRAINING, PlayerTime.today().minusYears(5), null, null, 60);

        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);
        MvcResult current = mockMvc.perform(get("/api/v1/players/" + profile.getId()).session(session))
                .andExpect(status().isOk())
                .andReturn();
        long version = objectMapper.readTree(current.getResponse().getContentAsString()).get("version").asLong();

        Map<String, Object> invalid = profileBody(player.getId(), false);
        invalid.put("version", version);
        invalid.put("birthDate", PlayerTime.today().minusYears(2).toString());
        mockMvc.perform(put("/api/v1/players/" + profile.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BIRTH_DATE_AFTER_HISTORY"));

        Map<String, Object> valid = profileBody(player.getId(), false);
        valid.put("version", version);
        valid.put("birthDate", PlayerTime.today().minusYears(6).toString());
        mockMvc.perform(put("/api/v1/players/" + profile.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(valid)))
                .andExpect(status().isOk());
    }

    @Test
    void birthDateCorrectionToMinorRecalculatesCompletenessAndTutorRequirement() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-birth-minor@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-birth-minor@example.com");
        Academy academy = academy("Academia Birth Minor", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, academy);
        User player = playerIn(academy, "player-birth-minor@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(20), LIMA, PlayerPosition.STRIKER);

        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);
        MvcResult current = mockMvc.perform(get("/api/v1/players/" + profile.getId()).session(adminSession))
                .andReturn();
        long version = objectMapper.readTree(current.getResponse().getContentAsString()).get("version").asLong();
        Map<String, Object> body = profileBody(player.getId(), true);
        body.put("version", version);
        mockMvc.perform(put("/api/v1/players/" + profile.getId())
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileStatus").value("INCOMPLETE"));

        MockHttpSession directorSession = login(director.getEmail());
        Csrf directorCsrf = csrf(directorSession);
        mockMvc.perform(put("/api/v1/players/" + profile.getId() + "/guardian")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(guardianBody(null))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/players/" + profile.getId()).session(directorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileStatus").value("COMPLETE"));
    }

    @Test
    void adultGuardianDataIsNotExposed() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-adult-guardian@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-adult-guardian@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-adult-guardian@example.com");
        Academy academy = academy("Academia Adulto", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, academy);
        User player = playerIn(academy, "player-adult-guardian@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(20), LIMA, PlayerPosition.STRIKER);
        createGuardian(profile, director);

        mockMvc.perform(get("/api/v1/players/" + profile.getId()).session(login(director.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guardianRequired").value(false))
                .andExpect(jsonPath("$.guardianDataStatus").value("NOT_APPLICABLE"))
                .andExpect(jsonPath("$.guardianConsentStatus").value("NOT_RECORDED"));
        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/guardian").session(login(director.getEmail())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUARDIAN_NOT_APPLICABLE"));
        mockMvc.perform(get("/api/v1/players/" + profile.getId() + "/guardian").session(login(scout.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUARDIAN_NOT_FOUND"));
    }

    @Test
    void profileUpdateUsesVersionAndRecomputesCompleteness() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-update@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-update@example.com");
        Academy academy = academy("Academia Update", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, academy);
        User player = playerIn(academy, "player-update@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(20), LIMA, PlayerPosition.STRIKER);

        MockHttpSession session = login(director.getEmail());
        Csrf csrf = csrf(session);
        MvcResult current = mockMvc.perform(get("/api/v1/players/" + profile.getId()).session(session))
                .andExpect(status().isOk())
                .andReturn();
        long version = objectMapper.readTree(current.getResponse().getContentAsString()).get("version").asLong();

        Map<String, Object> body = profileBody(player.getId(), false);
        body.put("version", version);
        body.put("heightCm", 180);
        body.put("weightKg", 70.00);
        mockMvc.perform(put("/api/v1/players/" + profile.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.heightCm").value(180))
                .andExpect(jsonPath("$.weightKg").value(70.0));

        mockMvc.perform(put("/api/v1/players/" + profile.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STALE_PLAYER_PROFILE"));
    }

    @Test
    void anyPlayerProfileBlocksRoleChangeEvenWhenInactive() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-role@example.com");
        Academy academy = academy("Academia Rol", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User player = playerIn(academy, "player-role@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);

        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);
        mockMvc.perform(patch("/api/v1/admin/users/" + player.getId() + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody("SCOUT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAYER_PROFILE_EXISTS"));

        mockMvc.perform(patch("/api/v1/players/" + profile.getId() + "/status")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/admin/users/" + player.getId() + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody("SCOUT"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAYER_PROFILE_EXISTS"));

        User accountWithoutProfile = user(Role.PLAYER, UserStatus.ACTIVE, "player-role-free@example.com");
        mockMvc.perform(patch("/api/v1/admin/users/" + accountWithoutProfile.getId() + "/assignment")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(assignmentBody("SCOUT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SCOUT"));
    }

    @Test
    void staffCannotReactivateProfileAndCsrfIsRequired() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-reactivate@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-reactivate@example.com");
        Academy academy = academy("Academia Reactivar", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        associate(director, academy);
        User player = playerIn(academy, "player-reactivate@example.com");
        PlayerProfile profile = createProfile(player, admin, true, PlayerTime.today().minusYears(16), LIMA, PlayerPosition.STRIKER);
        profile.setOperationalStatus(PlayerOperationalStatus.INACTIVE);
        playerProfileRepository.saveAndFlush(profile);

        MockHttpSession directorSession = login(director.getEmail());
        Csrf directorCsrf = csrf(directorSession);
        mockMvc.perform(patch("/api/v1/players/" + profile.getId() + "/status")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ONLY_ADMIN_CAN_REACTIVATE"));

        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);
        mockMvc.perform(patch("/api/v1/players/" + profile.getId() + "/status")
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/players")
                        .session(adminSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(profileBody(player.getId(), true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void postHistory(MockHttpSession session, Csrf csrf, PlayerProfile profile,
                             Map<String, Object> body, int expectedStatus, String expectedCode) throws Exception {
        mockMvc.perform(post("/api/v1/players/" + profile.getId() + "/history")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    private Map<String, Object> profileBody(Long userId, boolean minor) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("birthDate", (minor ? PlayerTime.today().minusYears(16) : PlayerTime.today().minusYears(20)).toString());
        body.put("residenceUbigeoCode", LIMA);
        body.put("heightCm", 170);
        body.put("weightKg", 65.00);
        body.put("position", "ATTACKING_MIDFIELDER");
        body.put("bestPosition", "LEFT_WINGER");
        body.put("dominantFoot", "RIGHT");
        body.put("summary", "Resumen del jugador.");
        body.put("photoUrl", "https://example.com/player.jpg");
        return body;
    }

    private Map<String, Object> guardianBody(Long version) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", "Maria");
        body.put("lastName", "Quispe");
        body.put("documentType", "DNI");
        body.put("documentNumber", "12345678");
        body.put("phone", "987654321");
        body.put("address", "Av. Ejemplo 123");
        body.put("relationship", "Madre");
        if (version != null) {
            body.put("version", version);
        }
        return body;
    }

    private Map<String, Object> assignmentBody(String role) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", role);
        body.put("academyId", null);
        body.put("clubId", null);
        body.put("position", null);
        return body;
    }

    private Map<String, Object> historyBody(ActivityType type, String teamGoals, String opponentGoals,
                                             String teamResult) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("activityType", type.name());
        body.put("activityDate", PlayerTime.today().minusDays(10).toString());
        body.put("competition", "Copa Ejemplo");
        body.put("opponent", "Club Ejemplo");
        body.put("ageCategory", "SUB_17");
        body.put("competitiveLevel", "NATIONAL");
        body.put("positionPlayed", "STRIKER");
        body.put("participation", "STARTER");
        body.put("minutesPlayed", 90);
        body.put("teamResult", teamResult);
        if (teamGoals != null) {
            int goals = Integer.parseInt(teamGoals);
            body.put("teamGoals", goals);
            body.put("opponentGoals", Integer.parseInt(opponentGoals));
            body.put("playerGoals", Math.min(1, goals));
            body.put("assists", Math.min(1, goals));
        }
        body.put("yellowCards", 0);
        body.put("redCards", 0);
        body.put("observations", "Observacion");
        body.put("ratings", Map.of("technical", 4, "tactical", 4, "physical", 4, "mental", 4, "overall", 4));
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
        academy.setCode("ACA-PLY-" + n);
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

    private PlayerProfile createProfile(User user, User operator, boolean complete,
                                        LocalDate birthDate, String residence, PlayerPosition position) {
        PlayerProfile profile = new PlayerProfile();
        profile.setUser(user);
        profile.setBirthDate(birthDate);
        profile.setResidenceUbigeoCode(residence);
        profile.setHeightCm(complete ? 170 : null);
        profile.setWeightKg(complete ? new BigDecimal("65.00") : null);
        profile.setPosition(position);
        profile.setBestPosition(position);
        profile.setDominantFoot(DominantFoot.RIGHT);
        profile.setSummary("Resumen");
        profile.setPhotoUrl("https://example.com/player.jpg");
        profile.setOperationalStatus(PlayerOperationalStatus.ACTIVE);
        profile.setProfileStatus(complete ? PlayerProfileStatus.COMPLETE : PlayerProfileStatus.INCOMPLETE);
        profile.setCompletedAt(complete ? Instant.now() : null);
        profile.setCreatedBy(operator);
        profile.setUpdatedBy(operator);
        return playerProfileRepository.saveAndFlush(profile);
    }

    private PlayerGuardian createGuardian(PlayerProfile profile, User operator) {
        PlayerGuardian guardian = new PlayerGuardian();
        guardian.setPlayerProfile(profile);
        guardian.setFirstName("Maria");
        guardian.setLastName("Quispe");
        guardian.setDocumentType(DocumentType.DNI);
        guardian.setDocumentNumber("12345678");
        guardian.setPhone("987654321");
        guardian.setAddress("Av. Ejemplo 123");
        guardian.setRelationship("Madre");
        guardian.setCreatedBy(operator);
        guardian.setUpdatedBy(operator);
        return guardianRepository.saveAndFlush(guardian);
    }

    private PlayerHistoryEntry createHistory(PlayerProfile profile, User operator, ActivityType type,
                                             LocalDate date, Participation participation,
                                             TeamResult result, Integer minutes) {
        PlayerHistoryEntry entry = new PlayerHistoryEntry();
        entry.setPlayerProfile(profile);
        entry.setActivityType(type);
        entry.setActivityDate(date);
        entry.setPositionPlayed(type.isMatch() ? PlayerPosition.STRIKER : null);
        entry.setParticipation(participation);
        entry.setMinutesPlayed(minutes);
        entry.setTeamResult(result);
        entry.setCreatedBy(operator);
        entry.setLastModifiedBy(operator);
        entry.setRevisionNumber(1);
        if (type.isMatch()) {
            entry.setOpponent("Rival");
            entry.setAgeCategory(AgeCategory.SUB_17);
            entry.setCompetitiveLevel(CompetitiveLevel.NATIONAL);
            entry.setTeamGoals(result == TeamResult.WIN ? 2 : 0);
            entry.setOpponentGoals(result == TeamResult.WIN ? 1 : 0);
            entry.setPlayerGoals(result == TeamResult.WIN && participation == Participation.STARTER ? 1 : null);
            entry.setOverallRating(4);
        }
        return historyRepository.saveAndFlush(entry);
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
