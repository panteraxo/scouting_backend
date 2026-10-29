package com.scouting.scouting.recommendation;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.GuardianConsentStatus;
import com.scouting.scouting.player.entity.PlayerGuardian;
import com.scouting.scouting.player.entity.PlayerGuardianConsentEvent;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.repository.PlayerGuardianConsentEventRepository;
import com.scouting.scouting.player.repository.PlayerGuardianRepository;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.player.dto.GuardianConsentUpdateRequest;
import com.scouting.scouting.player.service.PlayerGuardianService;
import com.scouting.scouting.player.service.PlayerTime;
import com.scouting.scouting.prospecting.entity.Prospect;
import com.scouting.scouting.prospecting.entity.ProspectEvaluation;
import com.scouting.scouting.prospecting.entity.ProspectStatus;
import com.scouting.scouting.prospecting.repository.ProspectEvaluationRepository;
import com.scouting.scouting.prospecting.repository.ProspectRepository;
import com.scouting.scouting.recommendation.entity.Recommendation;
import com.scouting.scouting.recommendation.entity.RecommendationAuthorizationType;
import com.scouting.scouting.recommendation.entity.RecommendationStatus;
import com.scouting.scouting.recommendation.dto.RecommendationCreateRequest;
import com.scouting.scouting.recommendation.service.RecommendationService;
import com.scouting.scouting.recommendation.repository.RecommendationEvaluationRepository;
import com.scouting.scouting.recommendation.repository.RecommendationHistoryEntryRepository;
import com.scouting.scouting.recommendation.repository.RecommendationRepository;
import com.scouting.scouting.recommendation.repository.RecommendationResponseRepository;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.security.AppUserPrincipal;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scouting_recommendations;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecommendationApiIntegrationTest {

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
    private ClubRepository clubRepository;
    @Autowired
    private UserOrganizationRepository userOrganizationRepository;
    @Autowired
    private PlayerProfileRepository playerProfileRepository;
    @Autowired
    private PlayerHistoryEntryRepository playerHistoryEntryRepository;
    @Autowired
    private PlayerGuardianConsentEventRepository consentRepository;
    @Autowired
    private PlayerGuardianRepository playerGuardianRepository;
    @Autowired
    private ProspectRepository prospectRepository;
    @Autowired
    private ProspectEvaluationRepository prospectEvaluationRepository;
    @Autowired
    private RecommendationRepository recommendationRepository;
    @Autowired
    private RecommendationResponseRepository recommendationResponseRepository;
    @Autowired
    private RecommendationHistoryEntryRepository recommendationHistoryEntryRepository;
    @Autowired
    private RecommendationEvaluationRepository recommendationEvaluationRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private PlayerGuardianService playerGuardianService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private final AtomicInteger sequence = new AtomicInteger(1);

    @BeforeEach
    void cleanDatabase() {
        recommendationResponseRepository.deleteAll();
        recommendationHistoryEntryRepository.deleteAll();
        recommendationEvaluationRepository.deleteAll();
        recommendationRepository.deleteAll();
        prospectEvaluationRepository.deleteAll();
        prospectRepository.deleteAll();
        playerHistoryEntryRepository.deleteAll();
        consentRepository.deleteAll();
        playerGuardianRepository.deleteAll();
        playerProfileRepository.deleteAll();
        userOrganizationRepository.deleteAll();
        clubRepository.deleteAll();
        academyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void scoutCreatesRecommendationAndSnapshotExcludesPrivateAndUnselectedData() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-create-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-create-rec@example.com");
        User manager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-create-rec@example.com");
        Academy academy = academy("Academia Create Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Create Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(manager, club);
        User playerUser = playerIn(academy, "player-create-rec@example.com");
        PlayerProfile player = createProfile(playerUser, admin, false);
        Prospect prospect = createProspect(scout, player);
        PlayerHistoryEntry selectedHistory = createHistory(player, admin);
        createHistory(player, admin);
        ProspectEvaluation selectedEvaluation = createEvaluation(prospect, scout, "evaluacion privada");
        ProspectEvaluation unselectedEvaluation = createEvaluation(prospect, scout, "no seleccionada");

        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        MvcResult created = mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), selectedHistory.getId(),
                                selectedEvaluation.getId()))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.message").value("Carta de recomendacion"))
                .andExpect(jsonPath("$.snapshot.player.firstName").value(playerUser.getFirstName()))
                .andExpect(jsonPath("$.snapshot.player.birthDate").doesNotExist())
                .andExpect(jsonPath("$.snapshot.player.email").doesNotExist())
                .andExpect(jsonPath("$.snapshot.player.documentNumber").doesNotExist())
                .andExpect(jsonPath("$.snapshot.player.phone").doesNotExist())
                .andExpect(jsonPath("$.snapshot.tutor").doesNotExist())
                .andExpect(jsonPath("$.snapshot.account").doesNotExist())
                .andExpect(jsonPath("$.snapshot.residence.districtCode").doesNotExist())
                .andExpect(jsonPath("$.snapshot.residence.departmentName").value("Lima"))
                .andExpect(jsonPath("$.snapshot.historyEntries.length()").value(1))
                .andExpect(jsonPath("$.snapshot.historyEntries[0].id").value(selectedHistory.getId()))
                .andExpect(jsonPath("$.snapshot.evaluations.length()").value(1))
                .andExpect(jsonPath("$.snapshot.evaluations[0].id").value(selectedEvaluation.getId()))
                .andExpect(jsonPath("$.snapshot.evaluations[0].privateNotes").doesNotExist())
                .andExpect(jsonPath("$.snapshot.evaluations[0].videoUrl").value("https://example.com/video"))
                .andExpect(jsonPath("$.snapshot.player.academy.verificationStatus").value("PENDING_VERIFICATION"))
                .andReturn();
        JsonNode createdJson = objectMapper.readTree(created.getResponse().getContentAsString());
        long recommendationId = createdJson.get("id").asLong();

        Recommendation stored = recommendationRepository.findById(recommendationId).orElseThrow();
        assertEquals(RecommendationAuthorizationType.SCOUT_SEND, stored.getAuthorizationType());
        assertEquals(RecommendationStatus.SENT, stored.getStatus());

        // El snapshot no cambia despues de modificar el perfil o la evaluacion.
        player.setSummary("Resumen cambiado");
        playerProfileRepository.saveAndFlush(player);
        selectedEvaluation.setStrengths("Fortaleza cambiada");
        prospectEvaluationRepository.saveAndFlush(selectedEvaluation);
        selectedHistory.setObservations("Observacion cambiada");
        playerHistoryEntryRepository.saveAndFlush(selectedHistory);

        mockMvc.perform(get("/api/v1/recommendations/" + recommendationId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshot.player.summary").value("Resumen original"))
                .andExpect(jsonPath("$.snapshot.historyEntries[0].observations").value("Observacion compartible"))
                .andExpect(jsonPath("$.snapshot.evaluations[0].strengths").value("Fortaleza"))
                .andExpect(jsonPath("$.snapshot.evaluations[0].privateNotes").doesNotExist());

        mockMvc.perform(get("/api/v1/clubs/me/recommendations/" + recommendationId)
                        .session(login(manager.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Carta de recomendacion"))
                .andExpect(jsonPath("$.snapshot.evaluations[0].privateNotes").doesNotExist())
                .andExpect(jsonPath("$.snapshot.evaluations[0].id").value(selectedEvaluation.getId()));

        assertNotNull(unselectedEvaluation.getId());
    }

    @Test
    void ownershipAndRoleMatrix() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-matrix-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-matrix-rec@example.com");
        User otherScout = user(Role.SCOUT, UserStatus.ACTIVE, "other-scout-matrix-rec@example.com");
        User professor = user(Role.PROFESSOR, UserStatus.ACTIVE, "professor-matrix-rec@example.com");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-matrix-rec@example.com");
        User playerUser = user(Role.PLAYER, UserStatus.ACTIVE, "player-matrix-rec@example.com");
        User manager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-matrix-rec@example.com");
        User otherManager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "other-manager-matrix-rec@example.com");
        Academy academy = academy("Academia Matrix Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Matrix Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club otherClub = club("Club Matrix Otro", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(manager, club);
        associateClub(otherManager, otherClub);
        associateAcademy(professor, academy);
        associateAcademy(director, academy);
        User player = playerIn(academy, "player-target-matrix-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);

        MockHttpSession scoutSession = login(scout.getEmail());
        long recommendationId = createRecommendation(scoutSession, csrf(scoutSession), prospect, club, List.of(), List.of());

        MockHttpSession otherScoutSession = login(otherScout.getEmail());
        mockMvc.perform(get("/api/v1/recommendations/" + recommendationId).session(otherScoutSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(otherScoutSession))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(otherScoutSession).header("X-CSRF-TOKEN", csrf(otherScoutSession).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/recommendations").session(login(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].message").doesNotExist())
                .andExpect(jsonPath("$.content[0].snapshot").doesNotExist());
        mockMvc.perform(get("/api/v1/admin/recommendations/" + recommendationId).session(login(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubName").value(club.getName()))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.snapshot").doesNotExist());
        mockMvc.perform(get("/api/v1/recommendations/" + recommendationId).session(login(admin.getEmail())))
                .andExpect(status().isForbidden());

        for (User denied : List.of(professor, director, playerUser)) {
            mockMvc.perform(get("/api/v1/recommendations/" + recommendationId).session(login(denied.getEmail())))
                    .andExpect(status().isForbidden());
        }
        for (User denied : List.of(scout, otherScout, professor, director, playerUser, manager)) {
            mockMvc.perform(get("/api/v1/admin/recommendations").session(login(denied.getEmail())))
                    .andExpect(status().isForbidden());
        }
        for (User denied : List.of(admin, scout, professor, director, playerUser)) {
            mockMvc.perform(get("/api/v1/clubs/me/recommendations").session(login(denied.getEmail())))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/clubs/me/recommendations/" + recommendationId)
                        .session(login(manager.getEmail())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/clubs/me/recommendations/" + recommendationId)
                        .session(login(otherManager.getEmail())))
                .andExpect(status().isNotFound());
        MockHttpSession otherManagerSession = login(otherManager.getEmail());
        Csrf otherManagerCsrf = csrf(otherManagerSession);
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(otherManagerSession)
                        .header("X-CSRF-TOKEN", otherManagerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "No es mi club"))))
                .andExpect(status().isNotFound());

        MockHttpSession managerSession = login(manager.getEmail());
        Csrf managerCsrf = csrf(managerSession);
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(managerSession)
                        .header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void minorRequiresCurrentGuardianConsentAndAdultRegistersScoutSend() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-minor-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-minor-rec@example.com");
        Academy academy = academy("Academia Minor Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Minor Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User guardianRecorder = user(Role.PROFESSOR, UserStatus.ACTIVE, "professor-minor-rec@example.com");
        associateAcademy(guardianRecorder, academy);

        User minorUser = playerIn(academy, "player-minor-rec@example.com");
        PlayerProfile minor = createProfile(minorUser, admin, true);
        Prospect minorProspect = createProspect(scout, minor);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);

        mockMvc.perform(post("/api/v1/prospects/" + minorProspect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUARDIAN_CONSENT_REQUIRED"));

        createConsent(minor, guardianRecorder, GuardianConsentStatus.REVOKED);
        mockMvc.perform(post("/api/v1/prospects/" + minorProspect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUARDIAN_CONSENT_REQUIRED"));

        createConsent(minor, guardianRecorder, GuardianConsentStatus.GRANTED);
        MvcResult minorCreated = mockMvc.perform(post("/api/v1/prospects/" + minorProspect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                .andExpect(status().isCreated())
                .andReturn();
        long minorRecommendationId = objectMapper.readTree(minorCreated.getResponse().getContentAsString())
                .get("id").asLong();
        assertEquals(RecommendationAuthorizationType.GUARDIAN_CONSENT,
                recommendationRepository.findById(minorRecommendationId).orElseThrow().getAuthorizationType());

        Club revokedClub = club("Club Revocado Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createConsent(minor, guardianRecorder, GuardianConsentStatus.REVOKED);
        mockMvc.perform(post("/api/v1/prospects/" + minorProspect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(revokedClub.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GUARDIAN_CONSENT_REQUIRED"));
        assertTrue(recommendationRepository.findByProspect_IdAndClub_Id(minorProspect.getId(), revokedClub.getId()).isEmpty());

        User adultUser = playerIn(academy, "player-adult-rec@example.com");
        PlayerProfile adult = createProfile(adultUser, admin, false);
        Prospect adultProspect = createProspect(scout, adult);
        Club adultClub = club("Club Adult Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        MvcResult adultCreated = mockMvc.perform(post("/api/v1/prospects/" + adultProspect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(adultClub.getId(), List.of(), List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snapshot.player.birthDate").doesNotExist())
                .andReturn();
        long adultRecommendationId = objectMapper.readTree(adultCreated.getResponse().getContentAsString())
                .get("id").asLong();
        assertEquals(RecommendationAuthorizationType.SCOUT_SEND,
                recommendationRepository.findById(adultRecommendationId).orElseThrow().getAuthorizationType());
    }

    @Test
    void clubMustBeActiveAndVerifiedAndDuplicateIsRejected() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-club-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-club-rec@example.com");
        Academy academy = academy("Academia Club Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club inactive = club("Club Inactivo Rec", OperationalStatus.INACTIVE, VerificationStatus.VERIFIED);
        Club unverified = club("Club No Verificado Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club rejected = club("Club Rechazado Rec", OperationalStatus.ACTIVE, VerificationStatus.REJECTED);
        Club valid = club("Club Valido Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User player = playerIn(academy, "player-club-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);

        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(inactive.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB_NOT_ELIGIBLE"));
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(unverified.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB_NOT_ELIGIBLE"));
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(rejected.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB_NOT_ELIGIBLE"));
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(999999L, List.of(), List.of()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CLUB_NOT_FOUND"));

        createRecommendation(session, csrf, prospect, valid, List.of(), List.of());
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(valid.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_ALREADY_EXISTS"));
    }

    @Test
    void recommendationRejectsPlayerWhoIsNoLongerEligible() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-ineligible-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-ineligible-rec@example.com");
        Academy academy = academy("Academia Ineligible Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Ineligible Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User player = playerIn(academy, "player-ineligible-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        profile.setOperationalStatus(PlayerOperationalStatus.INACTIVE);
        playerProfileRepository.saveAndFlush(profile);

        MockHttpSession session = login(scout.getEmail());
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf(session).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));
    }

    @Test
    void selectionMustBelongToPlayerAndProspectAndCanBeEmpty() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-selection-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-selection-rec@example.com");
        Academy academy = academy("Academia Selection Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Selection Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club emptyClub = club("Club Selection Empty", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club dedupeClub = club("Club Selection Dedupe", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User player1 = playerIn(academy, "player-selection-one@example.com");
        User player2 = playerIn(academy, "player-selection-two@example.com");
        PlayerProfile profile1 = createProfile(player1, admin, false);
        PlayerProfile profile2 = createProfile(player2, admin, false);
        Prospect prospect1 = createProspect(scout, profile1);
        Prospect prospect2 = createProspect(scout, profile2);
        PlayerHistoryEntry otherHistory = createHistory(profile2, admin);
        ProspectEvaluation otherEvaluation = createEvaluation(prospect2, scout, "privada");
        PlayerHistoryEntry ownHistory = createHistory(profile1, admin);
        ProspectEvaluation ownEvaluation = createEvaluation(prospect1, scout, "privada propia");
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);

        mockMvc.perform(post("/api/v1/prospects/" + prospect1.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), otherHistory.getId(), otherEvaluation.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SELECTION"));

        mockMvc.perform(post("/api/v1/prospects/" + prospect1.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(emptyClub.getId(), List.of(), List.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snapshot.historyEntries.length()").value(0))
                .andExpect(jsonPath("$.snapshot.evaluations.length()").value(0));

        mockMvc.perform(post("/api/v1/prospects/" + prospect1.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(dedupeClub.getId(),
                                List.of(ownHistory.getId(), ownHistory.getId()),
                                List.of(ownEvaluation.getId(), ownEvaluation.getId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.snapshot.historyEntries.length()").value(1))
                .andExpect(jsonPath("$.snapshot.historyEntries[0].id").value(ownHistory.getId()))
                .andExpect(jsonPath("$.snapshot.evaluations.length()").value(1))
                .andExpect(jsonPath("$.snapshot.evaluations[0].id").value(ownEvaluation.getId()));
    }

    @Test
    void clubResponsesAreAppendOnlyAndUpdateStatus() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-response-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-response-rec@example.com");
        User manager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-response-rec@example.com");
        Academy academy = academy("Academia Response Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Response Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(manager, club);
        User player = playerIn(academy, "player-response-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        MockHttpSession scoutSession = login(scout.getEmail());
        long recommendationId = createRecommendation(scoutSession, csrf(scoutSession), prospect, club, List.of(), List.of());
        MockHttpSession managerSession = login(manager.getEmail());
        Csrf managerCsrf = csrf(managerSession);

        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "Nos interesa"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESPONDED"))
                .andExpect(jsonPath("$.responses.length()").value(1))
                .andExpect(jsonPath("$.responses[0].status").value("INTERESTED"));
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INFO_REQUESTED", "message", "Mas informacion"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.responses.length()").value(2))
                .andExpect(jsonPath("$.responses[0].status").value("INFO_REQUESTED"));

        assertEquals(2, recommendationResponseRepository.count());
        Recommendation stored = recommendationRepository.findById(recommendationId).orElseThrow();
        assertEquals(RecommendationStatus.RESPONDED, stored.getStatus());

        mockMvc.perform(get("/api/v1/clubs/me/recommendations/" + recommendationId).session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responses.length()").value(2));
        mockMvc.perform(get("/api/v1/recommendations/" + recommendationId).session(scoutSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responses.length()").value(2));
        mockMvc.perform(delete("/api/v1/recommendations/" + recommendationId)
                        .session(scoutSession).header("X-CSRF-TOKEN", csrf(scoutSession).token()))
                .andExpect(status().isMethodNotAllowed());
        mockMvc.perform(delete("/api/v1/clubs/me/recommendations/" + recommendationId)
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token()))
                .andExpect(status().isMethodNotAllowed());
        MockHttpSession adminSession = login(admin.getEmail());
        mockMvc.perform(delete("/api/v1/admin/recommendations/" + recommendationId)
                        .session(adminSession).header("X-CSRF-TOKEN", csrf(adminSession).token()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void inactiveClubCannotRespondButCanReadAndActiveClubCanRespond() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-inactive-club-response@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-inactive-club-response@example.com");
        User manager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-inactive-club-response@example.com");
        Academy academy = academy("Academia Inactive Club Response", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Inactive Response", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(manager, club);
        User player = playerIn(academy, "player-inactive-club-response@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        MockHttpSession scoutSession = login(scout.getEmail());
        long recommendationId = createRecommendation(scoutSession, csrf(scoutSession), prospect, club,
                List.of(), List.of());
        MockHttpSession managerSession = login(manager.getEmail());
        Csrf managerCsrf = csrf(managerSession);

        club.setOperationalStatus(OperationalStatus.INACTIVE);
        club = clubRepository.saveAndFlush(club);

        mockMvc.perform(get("/api/v1/clubs/me/recommendations/" + recommendationId).session(managerSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/clubs/me/recommendations").session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "No deberia responder"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLUB_INACTIVE"));
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/999999/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "No existe"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECOMMENDATION_NOT_FOUND"));

        club.setOperationalStatus(OperationalStatus.ACTIVE);
        clubRepository.saveAndFlush(club);
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "Ahora si responde"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESPONDED"));
    }

    @Test
    void responseMessageValidationRejectsEmptyAndOversizedValues() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-response-validation@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-response-validation@example.com");
        User manager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-response-validation@example.com");
        Academy academy = academy("Academia Response Validation", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Response Validation", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(manager, club);
        User player = playerIn(academy, "player-response-validation@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        MockHttpSession scoutSession = login(scout.getEmail());
        long recommendationId = createRecommendation(scoutSession, csrf(scoutSession), prospect, club,
                List.of(), List.of());
        MockHttpSession managerSession = login(manager.getEmail());
        Csrf managerCsrf = csrf(managerSession);

        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", managerCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "x".repeat(4001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void scoutListsOwnRecommendationsWithPaginationOrderAndLatestResponse() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-list-scout-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-list-scout-rec@example.com");
        User otherScout = user(Role.SCOUT, UserStatus.ACTIVE, "other-scout-list-rec@example.com");
        User managerOne = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-one-list-rec@example.com");
        User managerTwo = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-two-list-rec@example.com");
        Academy academy = academy("Academia List Scout Rec", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Club clubOne = club("Club List One", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club clubTwo = club("Club List Two", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club otherClub = club("Club List Other", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(managerOne, clubOne);
        associateClub(managerTwo, clubTwo);
        User player = playerIn(academy, "player-list-scout-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        Prospect otherProspect = createProspect(otherScout, profile);

        MockHttpSession scoutSession = login(scout.getEmail());
        Csrf scoutCsrf = csrf(scoutSession);
        long older = createRecommendation(scoutSession, scoutCsrf, prospect, clubOne, List.of(), List.of());
        Thread.sleep(15);
        long newer = createRecommendation(scoutSession, scoutCsrf, prospect, clubTwo, List.of(), List.of());

        MockHttpSession otherScoutSession = login(otherScout.getEmail());
        long otherRecommendation = createRecommendation(otherScoutSession, csrf(otherScoutSession),
                otherProspect, otherClub, List.of(), List.of());

        MockHttpSession managerTwoSession = login(managerTwo.getEmail());
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + newer + "/responses")
                        .session(managerTwoSession).header("X-CSRF-TOKEN", csrf(managerTwoSession).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "Respuesta reciente"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/prospects/" + prospect.getId() + "/recommendations?page=0&size=1")
                        .session(scoutSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(newer))
                .andExpect(jsonPath("$.content[0].club.id").value(clubTwo.getId()))
                .andExpect(jsonPath("$.content[0].player.id").value(profile.getId()))
                .andExpect(jsonPath("$.content[0].scout.id").value(scout.getId()))
                .andExpect(jsonPath("$.content[0].lastResponseStatus").value("INTERESTED"))
                .andExpect(jsonPath("$.content[0].lastResponseAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].message").doesNotExist())
                .andExpect(jsonPath("$.content[0].snapshot").doesNotExist());

        mockMvc.perform(get("/api/v1/prospects/" + prospect.getId() + "/recommendations?page=1&size=1")
                        .session(scoutSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(older))
                .andExpect(jsonPath("$.content[0].lastResponseStatus").value(nullValue()));

        mockMvc.perform(get("/api/v1/prospects/" + otherProspect.getId() + "/recommendations")
                        .session(otherScoutSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(otherRecommendation))
                .andExpect(jsonPath("$.content[0].club.id").value(otherClub.getId()));
    }

    @Test
    void clubListsOnlyItsOwnRecommendationsWithSummaryAndLatestResponse() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-list-club-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-list-club-rec@example.com");
        User managerOne = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-one-club-list@example.com");
        User managerTwo = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-two-club-list@example.com");
        Academy academy = academy("Academia List Club Rec", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Club clubOne = club("Club Own List One", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club clubTwo = club("Club Own List Two", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(managerOne, clubOne);
        associateClub(managerTwo, clubTwo);
        User player = playerIn(academy, "player-list-club-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        MockHttpSession scoutSession = login(scout.getEmail());
        long ownRecommendation = createRecommendation(scoutSession, csrf(scoutSession), prospect, clubOne,
                List.of(), List.of());
        long otherRecommendation = createRecommendation(scoutSession, csrf(scoutSession), prospect, clubTwo,
                List.of(), List.of());

        MockHttpSession managerOneSession = login(managerOne.getEmail());
        MockHttpSession managerTwoSession = login(managerTwo.getEmail());

        mockMvc.perform(get("/api/v1/clubs/me/recommendations").session(managerOneSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(ownRecommendation))
                .andExpect(jsonPath("$.content[0].club.id").value(clubOne.getId()))
                .andExpect(jsonPath("$.content[0].player.id").value(profile.getId()))
                .andExpect(jsonPath("$.content[0].scout.id").value(scout.getId()))
                .andExpect(jsonPath("$.content[0].lastResponseStatus").value(nullValue()))
                .andExpect(jsonPath("$.content[0].message").doesNotExist())
                .andExpect(jsonPath("$.content[0].snapshot").doesNotExist());
        mockMvc.perform(get("/api/v1/clubs/me/recommendations").session(managerTwoSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(otherRecommendation))
                .andExpect(jsonPath("$.content[0].club.id").value(clubTwo.getId()));

        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + ownRecommendation + "/responses")
                        .session(managerOneSession).header("X-CSRF-TOKEN", csrf(managerOneSession).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INFO_REQUESTED", "message", "Solicito informacion"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/clubs/me/recommendations").session(managerOneSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastResponseStatus").value("INFO_REQUESTED"))
                .andExpect(jsonPath("$.content[0].lastResponseAt").isNotEmpty());
        mockMvc.perform(get("/api/v1/clubs/me/recommendations").session(managerTwoSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lastResponseStatus").value(nullValue()));
    }

    @Test
    void recommendationRejectsIncompleteProfileInactiveAccountAndInactiveAcademy() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-eligibility-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-eligibility-rec@example.com");
        Academy academy = academy("Academia Eligibility Rec", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Academy inactiveAcademy = academy("Academia Eligibility Inactive", OperationalStatus.INACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Eligibility Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);

        PlayerProfile incomplete = createProfile(playerIn(academy, "player-incomplete-rec@example.com"),
                admin, false);
        incomplete.setProfileStatus(PlayerProfileStatus.INCOMPLETE);
        playerProfileRepository.saveAndFlush(incomplete);
        Prospect incompleteProspect = createProspect(scout, incomplete);

        User inactiveAccount = playerIn(academy, "player-inactive-account-rec@example.com");
        PlayerProfile inactiveAccountProfile = createProfile(inactiveAccount, admin, false);
        inactiveAccount.setStatus(UserStatus.INACTIVE);
        userRepository.saveAndFlush(inactiveAccount);
        Prospect inactiveAccountProspect = createProspect(scout, inactiveAccountProfile);

        User inactiveAcademyPlayer = playerIn(inactiveAcademy, "player-inactive-academy-rec@example.com");
        PlayerProfile inactiveAcademyProfile = createProfile(inactiveAcademyPlayer, admin, false);
        Prospect inactiveAcademyProspect = createProspect(scout, inactiveAcademyProfile);

        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);
        for (Prospect ineligible : List.of(incompleteProspect, inactiveAccountProspect, inactiveAcademyProspect)) {
            mockMvc.perform(post("/api/v1/prospects/" + ineligible.getId() + "/recommendations")
                            .session(session).header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PLAYER_NOT_ELIGIBLE"));
        }
    }

    @Test
    void createMessageValidationRejectsBlankOversizedAndMissingClub() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-create-validation-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-create-validation-rec@example.com");
        Academy academy = academy("Academia Create Validation", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club Create Validation", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User player = playerIn(academy, "player-create-validation-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        MockHttpSession session = login(scout.getEmail());
        Csrf csrf = csrf(session);

        for (String message : List.of("   ", "x".repeat(4001))) {
            mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                            .session(session).header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("clubId", club.getId(), "message", message))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("message", "Carta sin club"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void consentRevocationBlocksConcurrentRecommendationSend() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-concurrent-consent@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-concurrent-consent@example.com");
        User professor = user(Role.PROFESSOR, UserStatus.ACTIVE, "professor-concurrent-consent@example.com");
        Academy academy = academy("Academia Concurrent Consent", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        associateAcademy(professor, academy);
        Club club = club("Club Concurrent Consent", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User minorUser = playerIn(academy, "player-concurrent-consent@example.com");
        PlayerProfile minor = createProfile(minorUser, admin, true);
        createGuardian(minor, professor);
        createConsent(minor, professor, GuardianConsentStatus.GRANTED);
        Prospect prospect = createProspect(scout, minor);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<Integer> revoke = executor.submit(() -> transactionTemplate.execute(status -> {
                playerProfileRepository.findByIdForUpdate(minor.getId()).orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se libero el bloqueo de prueba.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                playerGuardianService.updateConsent(
                        AppUserPrincipal.from(professor),
                        minor.getId(),
                        new GuardianConsentUpdateRequest(GuardianConsentStatus.REVOKED, null,
                                "revocacion concurrente"));
                return 1;
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));

            Future<String> send = executor.submit(() -> {
                try {
                    recommendationService.create(AppUserPrincipal.from(scout), prospect.getId(),
                            new RecommendationCreateRequest(club.getId(), "Carta concurrente",
                                    List.of(), List.of()));
                    return "CREATED";
                } catch (ApiException ex) {
                    return ex.getCode();
                }
            });

            Thread.sleep(250);
            assertFalse(send.isDone(), "El envio debe esperar el bloqueo del perfil.");

            release.countDown();
            assertEquals(1, revoke.get(10, TimeUnit.SECONDS));
            assertEquals("GUARDIAN_CONSENT_REQUIRED", send.get(10, TimeUnit.SECONDS));
            assertTrue(recommendationRepository
                    .findByProspect_IdAndClub_Id(prospect.getId(), club.getId()).isEmpty());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void consentUpdateWaitsForProfileLock() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-consent-lock@example.com");
        User professor = user(Role.PROFESSOR, UserStatus.ACTIVE, "professor-consent-lock@example.com");
        Academy academy = academy("Academia Consent Lock", OperationalStatus.ACTIVE,
                VerificationStatus.PENDING_VERIFICATION);
        associateAcademy(professor, academy);
        User minorUser = playerIn(academy, "player-consent-lock@example.com");
        PlayerProfile minor = createProfile(minorUser, admin, true);
        createGuardian(minor, professor);
        createConsent(minor, professor, GuardianConsentStatus.GRANTED);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            Future<Integer> hold = executor.submit(() -> transactionTemplate.execute(status -> {
                playerProfileRepository.findByIdForUpdate(minor.getId()).orElseThrow();
                locked.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se libero el bloqueo de prueba.");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ex);
                }
                return 1;
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));

            Future<?> update = executor.submit(() -> playerGuardianService.updateConsent(
                    AppUserPrincipal.from(professor),
                    minor.getId(),
                    new GuardianConsentUpdateRequest(GuardianConsentStatus.REVOKED, null,
                            "revocacion bloqueada")));

            Thread.sleep(250);
            assertFalse(update.isDone(),
                    "La revocacion debe esperar el bloqueo del perfil.");

            release.countDown();
            assertEquals(1, hold.get(10, TimeUnit.SECONDS));
            update.get(10, TimeUnit.SECONDS);
            assertEquals(GuardianConsentStatus.REVOKED,
                    consentRepository.findTopByPlayerProfile_IdOrderByRecordedAtDescIdDesc(minor.getId())
                            .orElseThrow().getStatus());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void csrfAndNoStoreOnSuccessAndErrors() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-csrf-rec@example.com");
        User scout = user(Role.SCOUT, UserStatus.ACTIVE, "scout-csrf-rec@example.com");
        User manager = user(Role.CLUB_MANAGER, UserStatus.ACTIVE, "manager-csrf-rec@example.com");
        Academy academy = academy("Academia CSRF Rec", OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Club club = club("Club CSRF Rec", OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        associateClub(manager, club);
        User player = playerIn(academy, "player-csrf-rec@example.com");
        PlayerProfile profile = createProfile(player, admin, false);
        Prospect prospect = createProspect(scout, profile);
        MockHttpSession scoutSession = login(scout.getEmail());

        mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(scoutSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), List.of(), List.of()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        long recommendationId = createRecommendation(scoutSession, csrf(scoutSession), prospect, club,
                List.of(), List.of());
        MockHttpSession managerSession = login(manager.getEmail());
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "Sin CSRF"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(post("/api/v1/clubs/me/recommendations/" + recommendationId + "/responses")
                        .session(managerSession).header("X-CSRF-TOKEN", csrf(managerSession).token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INTERESTED", "message", "Con CSRF"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        mockMvc.perform(get("/api/v1/prospects/" + prospect.getId() + "/recommendations").session(scoutSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(get("/api/v1/clubs/me/recommendations").session(login(manager.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(get("/api/v1/admin/recommendations").session(login(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
        mockMvc.perform(get("/api/v1/recommendations/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private long createRecommendation(MockHttpSession session, Csrf csrf, Prospect prospect,
                                      Club club, List<Long> historyIds, List<Long> evaluationIds) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/prospects/" + prospect.getId() + "/recommendations")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(recommendationBody(club.getId(), historyIds, evaluationIds))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private Map<String, Object> recommendationBody(Long clubId, Long historyId, Long evaluationId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clubId", clubId);
        body.put("message", "Carta de recomendacion");
        body.put("historyEntryIds", historyId == null ? List.of() : List.of(historyId));
        body.put("evaluationIds", evaluationId == null ? List.of() : List.of(evaluationId));
        return body;
    }

    private Map<String, Object> recommendationBody(Long clubId, List<Long> historyIds, List<Long> evaluationIds) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("clubId", clubId);
        body.put("message", "Carta de recomendacion");
        body.put("historyEntryIds", historyIds == null ? List.of() : historyIds);
        body.put("evaluationIds", evaluationIds == null ? List.of() : evaluationIds);
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
        associateAcademy(player, academy);
        return player;
    }

    private Academy academy(String name, OperationalStatus operational, VerificationStatus verification) {
        int n = sequence.incrementAndGet();
        Academy academy = new Academy();
        academy.setCode("ACA-REC-" + n);
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

    private Club club(String name, OperationalStatus operational, VerificationStatus verification) {
        int n = sequence.incrementAndGet();
        Club club = new Club();
        club.setCode("CLU-REC-" + n);
        club.setName(name);
        club.setType("Club de futbol");
        club.setRuc(String.format("20%09d", n + 100));
        club.setFoundationYear(1998);
        club.setLogoUrl("https://cdn.example.com/club.png");
        club.setDescription("Descripcion institucional del club.");
        club.setCountryCode("PE");
        club.setUbigeoCode(LIMA);
        club.setAddress("Av. Institucional 456");
        club.setInstitutionalEmail("contacto@club.example.com");
        club.setInstitutionalPhone("+51 1 555 0202");
        club.setOperationalStatus(operational);
        club.setVerificationStatus(verification);
        return clubRepository.saveAndFlush(club);
    }

    private UserOrganization associateAcademy(User user, Academy academy) {
        return saveAssociation(user, academy, null);
    }

    private UserOrganization associateClub(User user, Club club) {
        return saveAssociation(user, null, club);
    }

    private UserOrganization saveAssociation(User user, Academy academy, Club club) {
        UserOrganization association = new UserOrganization();
        association.setUser(user);
        association.setAcademy(academy);
        association.setClub(club);
        association.setPosition(null);
        return userOrganizationRepository.saveAndFlush(association);
    }

    private PlayerProfile createProfile(User user, User operator, boolean minor) {
        PlayerProfile profile = new PlayerProfile();
        profile.setUser(user);
        profile.setBirthDate(PlayerTime.today().minusYears(minor ? 16 : 20));
        profile.setResidenceUbigeoCode(LIMA);
        profile.setHeightCm(175);
        profile.setWeightKg(new BigDecimal("70.00"));
        profile.setPosition(PlayerPosition.STRIKER);
        profile.setBestPosition(PlayerPosition.SECOND_STRIKER);
        profile.setDominantFoot(DominantFoot.RIGHT);
        profile.setSummary("Resumen original");
        profile.setPhotoUrl("https://example.com/player.jpg");
        profile.setOperationalStatus(PlayerOperationalStatus.ACTIVE);
        profile.setProfileStatus(PlayerProfileStatus.COMPLETE);
        profile.setCompletedAt(Instant.now());
        profile.setCreatedBy(operator);
        profile.setUpdatedBy(operator);
        return playerProfileRepository.saveAndFlush(profile);
    }

    private Prospect createProspect(User scout, PlayerProfile player) {
        Instant now = Instant.now();
        Prospect prospect = new Prospect();
        prospect.setScout(scout);
        prospect.setPlayer(player);
        prospect.setStatus(ProspectStatus.FOLLOWING);
        prospect.setPrivateNotes("nota privada del prospecto");
        prospect.setStartedAt(now);
        prospect.setLastActivityAt(now);
        return prospectRepository.saveAndFlush(prospect);
    }

    private PlayerHistoryEntry createHistory(PlayerProfile profile, User operator) {
        PlayerHistoryEntry history = new PlayerHistoryEntry();
        history.setPlayerProfile(profile);
        history.setActivityType(ActivityType.TRAINING);
        history.setActivityDate(PlayerTime.today().minusMonths(2));
        history.setObservations("Observacion compartible");
        history.setStrengths("Fortaleza");
        history.setImprovements("Mejora");
        history.setRecommendations("Recomendacion historica");
        history.setCreatedBy(operator);
        history.setLastModifiedBy(operator);
        history.setRevisionNumber(1);
        return playerHistoryEntryRepository.saveAndFlush(history);
    }

    private ProspectEvaluation createEvaluation(Prospect prospect, User scout, String privateNotes) {
        ProspectEvaluation evaluation = new ProspectEvaluation();
        evaluation.setProspect(prospect);
        evaluation.setCreatedBy(scout);
        evaluation.setEvaluatedAt(PlayerTime.today().minusDays(5));
        evaluation.setTechnicalRating(4);
        evaluation.setTacticalRating(4);
        evaluation.setPhysicalRating(4);
        evaluation.setMentalRating(4);
        evaluation.setOverallRating(4);
        evaluation.setStrengths("Fortaleza");
        evaluation.setImprovements("Mejora");
        evaluation.setRecommendation("Seguir");
        evaluation.setPrivateNotes(privateNotes);
        evaluation.setVideoUrl("https://example.com/video");
        return prospectEvaluationRepository.saveAndFlush(evaluation);
    }

    private PlayerGuardian createGuardian(PlayerProfile profile, User actor) {
        PlayerGuardian guardian = new PlayerGuardian();
        guardian.setPlayerProfile(profile);
        guardian.setFirstName("Tutor");
        guardian.setLastName("Legal");
        guardian.setDocumentType(DocumentType.DNI);
        guardian.setDocumentNumber("TUTOR" + sequence.incrementAndGet());
        guardian.setPhone("999888777");
        guardian.setAddress("Av. Tutor 123");
        guardian.setRelationship("Padre");
        guardian.setCreatedBy(actor);
        return playerGuardianRepository.saveAndFlush(guardian);
    }

    private PlayerGuardianConsentEvent createConsent(PlayerProfile profile, User actor,
                                                     GuardianConsentStatus status) {
        PlayerGuardianConsentEvent event = new PlayerGuardianConsentEvent();
        event.setPlayerProfile(profile);
        event.setStatus(status);
        event.setRecordedAt(Instant.now());
        event.setRecordedBy(actor);
        return consentRepository.saveAndFlush(event);
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
