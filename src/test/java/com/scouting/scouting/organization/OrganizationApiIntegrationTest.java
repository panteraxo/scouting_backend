package com.scouting.scouting.organization;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.organization.repository.OrganizationCodeSequenceRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.organization.service.OrganizationDataIntegrity;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scouting_organizations;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationApiIntegrationTest {

    private static final String PASSWORD = "Password123!";
    private static final String VALID_UBIGEO = "150101";

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
    // Administracion
    // ---------------------------------------------------------------------

    @Test
    void adminCreatesAcademyWithServerDefaultsIgnoringClientControlledFields() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-create@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> body = academyBody("Academia Uno", nextRuc());
        body.put("id", 999);
        body.put("code", "ACA-999999");
        body.put("operationalStatus", "INACTIVE");
        body.put("verificationStatus", "VERIFIED");
        body.put("counts", Map.of("players", 10));
        body.put("representativeEmail", "privado@example.com");

        MvcResult result = mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value("ACA-000001"))
                .andExpect(jsonPath("$.operationalStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.counts.players").value(nullValue()))
                .andExpect(jsonPath("$.counts.prospects").value(nullValue()))
                .andExpect(jsonPath("$.counts.professors").value(nullValue()))
                .andExpect(jsonPath("$.counts.teams").value(nullValue()))
                .andExpect(jsonPath("$.representativeEmail").doesNotExist())
                .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        long firstId = created.get("id").asLong();
        Academy stored = academyRepository.findById(firstId).orElseThrow();
        assertEquals("ACA-000001", stored.getCode());
        assertEquals(OperationalStatus.ACTIVE, stored.getOperationalStatus());
        assertEquals(VerificationStatus.PENDING_VERIFICATION, stored.getVerificationStatus());

        // El segundo codigo es secuencial y no se repite.
        MvcResult second = mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Academia Dos", nextRuc()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ACA-000002"))
                .andReturn();
        assertNotEquals("ACA-000001", objectMapper.readTree(second.getResponse().getContentAsString()).get("code").asText());

        // La vista administrativa si incluye RUC y representante.
        mockMvc.perform(get("/api/v1/admin/academies").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ruc").isNotEmpty())
                .andExpect(jsonPath("$.content[0].representativeName").value("Ana Torres"));

        // El codigo es inmutable ante una edicion administrativa.
        mockMvc.perform(put("/api/v1/admin/academies/" + firstId)
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Academia Uno Editada", stored.getRuc()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ACA-000001"))
                .andExpect(jsonPath("$.name").value("Academia Uno Editada"));
    }

    @Test
    void academyCanBeCreatedWithoutRepresentativeName() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-no-representative@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> body = academyBody("Academia Sin Representante", nextRuc());
        body.remove("representativeName");

        MvcResult result = mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.representativeName").value(nullValue()))
                .andReturn();

        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertEquals("", academyRepository.findById(id).orElseThrow().getRepresentativeName());
    }

    @Test
    void academyRepresentativeNameIsDerivedFromActiveDirectorAccount() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-representative@example.com");
        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);

        Map<String, Object> body = academyBody("Academia Con Director", nextRuc());
        body.remove("representativeName");

        MvcResult created = mockMvc.perform(post("/api/v1/admin/academies")
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.representativeName").value(nullValue()))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        Academy academy = academyRepository.findById(id).orElseThrow();

        User director = createUser(Role.ACADEMY_DIRECTOR,
                "director-representative@example.com", "Lucia", "Ramirez");
        associate(director, academy);

        mockMvc.perform(get("/api/v1/admin/academies/" + id).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.representativeName").value("Lucia Ramirez"));

        verify(adminSession, adminCsrf, id, "VERIFIED", 200);

        // Un envio identico sin representante no reinicia la verificacion.
        MockHttpSession directorSession = login(director.getEmail());
        Csrf directorCsrf = csrf(directorSession);
        mockMvc.perform(put("/api/v1/academies/me")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.representativeName").value("Lucia Ramirez"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));
    }

    @Test
    void academyWithoutDirectorKeepsLegacyRepresentativeName() throws Exception {
        Academy academy = createAcademyEntity("Academia Legacy", "ACA-T-LEG-1",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User admin = createUser(Role.ADMIN, "admin-legacy-representative@example.com");
        MockHttpSession session = login(admin.getEmail());

        mockMvc.perform(get("/api/v1/admin/academies/" + academy.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.representativeName").value("Ana Torres"));
    }

    @Test
    void adminCreatesClubWithFixedCountryAndIndependentCodeSequence() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-club-create@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> body = clubBody("Club Uno", nextRuc());
        body.put("countryCode", "US");
        body.put("operationalStatus", "INACTIVE");
        body.put("verificationStatus", "VERIFIED");
        body.put("code", "CLU-999999");

        mockMvc.perform(post("/api/v1/admin/clubs")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("CLU-000001"))
                .andExpect(jsonPath("$.countryCode").value("PE"))
                .andExpect(jsonPath("$.operationalStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"))
                .andExpect(jsonPath("$.responsibleNames").isEmpty())
                .andExpect(jsonPath("$.counts.players").value(nullValue()));

        // La secuencia de club es independiente de la de academia.
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Academia Secuencia", nextRuc()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("ACA-000001"));

        mockMvc.perform(get("/api/v1/admin/clubs").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].ruc").isNotEmpty());
    }

    @Test
    void adminControlsVerificationAndOperationalTransitions() throws Exception {
        Academy academy = createAcademyEntity("Academia Estados", "ACA-T-100",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        User admin = createUser(Role.ADMIN, "admin-states@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        verify(session, csrf, academy.getId(), "VERIFIED", 200);
        mockMvc.perform(patch("/api/v1/admin/academies/" + academy.getId() + "/deactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("INACTIVE"));
        // La verificacion es independiente de la vigencia operativa.
        verify(session, csrf, academy.getId(), "PENDING_VERIFICATION", 200);
        mockMvc.perform(patch("/api/v1/admin/academies/" + academy.getId() + "/reactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("ACTIVE"));
        verify(session, csrf, academy.getId(), "REJECTED", 200);
        verify(session, csrf, academy.getId(), "PENDING_VERIFICATION", 200);
        verify(session, csrf, academy.getId(), "VERIFIED", 200);

        // Una edicion ADMIN no reinicia la verificacion.
        mockMvc.perform(put("/api/v1/admin/academies/" + academy.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Academia Estados Editada", academy.getRuc()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Academia Estados Editada"))
                .andExpect(jsonPath("$.verificationStatus").value("VERIFIED"));

        // VERIFIED no puede pasar directamente a REJECTED.
        mockMvc.perform(patch("/api/v1/admin/academies/" + academy.getId() + "/verification")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_VERIFICATION_TRANSITION"));

        // ADMIN puede verificar una organizacion inactiva.
        mockMvc.perform(patch("/api/v1/admin/academies/" + academy.getId() + "/deactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk());
        verify(session, csrf, academy.getId(), "PENDING_VERIFICATION", 200);

        // No existe endpoint DELETE; la baja es logica.
        mockMvc.perform(delete("/api/v1/admin/academies/" + academy.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().is4xxClientError());
        assertTrue(academyRepository.findById(academy.getId()).isPresent());
    }

    @Test
    void nonAdminRolesCannotUseAdminEndpoints() throws Exception {
        List<Role> roles = List.of(Role.ACADEMY_DIRECTOR, Role.PROFESSOR, Role.CLUB_MANAGER, Role.SCOUT, Role.PLAYER);
        for (Role role : roles) {
            User user = createUser(role, role.name().toLowerCase() + "-admin-denied@example.com");
            MockHttpSession session = login(user.getEmail());
            mockMvc.perform(get("/api/v1/admin/academies").session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            mockMvc.perform(get("/api/v1/admin/clubs").session(session))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
        mockMvc.perform(get("/api/v1/admin/academies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writesRequireCsrfAndUnknownSortIsRejected() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-csrf@example.com");
        MockHttpSession session = login(admin.getEmail());

        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Sin CSRF", nextRuc()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        Csrf csrf = csrf(session);
        MvcResult created = mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Con CSRF", nextRuc()))))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/academies/" + id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Sin CSRF PUT", nextRuc()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        mockMvc.perform(patch("/api/v1/admin/academies/" + id + "/deactivate")
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_INVALID"));

        mockMvc.perform(get("/api/v1/admin/academies")
                        .session(session)
                        .param("sort", "passwordHash,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT"));

        mockMvc.perform(get("/api/v1/admin/academies")
                        .session(session)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void adminListSupportsPaginationFiltersAndWhitelistedSort() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-page@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        for (String name : List.of("Zeta", "Alfa", "Beta")) {
            mockMvc.perform(post("/api/v1/admin/academies")
                            .session(session).header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(academyBody(name, nextRuc()))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/admin/academies").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/admin/academies")
                        .session(session)
                        .param("size", "2")
                        .param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Zeta"));

        mockMvc.perform(get("/api/v1/admin/academies")
                        .session(session).param("q", "alfa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alfa"));

        mockMvc.perform(get("/api/v1/admin/academies")
                        .session(session).param("ubigeo", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/admin/academies")
                        .session(session).param("ubigeo", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UBIGEO_FILTER"));
    }

    @Test
    void adminSearchTreatsLikeWildcardsAndEscapeAsLiteralCharacters() throws Exception {
        createAcademyEntity("Academia Cien %", "ACA-T-600",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createAcademyEntity("Academia Guion_Bajo", "ACA-T-601",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createAcademyEntity("Academia Exclamacion!", "ACA-T-602",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createAcademyEntity("Academia Normal", "ACA-T-603",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);

        createClubEntity("Club Cien %", "CLU-T-600",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createClubEntity("Club Guion_Bajo", "CLU-T-601",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createClubEntity("Club Exclamacion!", "CLU-T-602",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createClubEntity("Club Normal", "CLU-T-603",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);

        User admin = createUser(Role.ADMIN, "admin-literal-search@example.com");
        MockHttpSession session = login(admin.getEmail());

        // Un '%' literal no actua como comodin: solo coincide el nombre que lo contiene.
        mockMvc.perform(get("/api/v1/admin/academies").session(session).param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Academia Cien %"));

        // Un '_' literal tampoco coincide con cualquier caracter.
        mockMvc.perform(get("/api/v1/admin/academies").session(session).param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Academia Guion_Bajo"));

        // El caracter de escape '!' tambien se busca literalmente.
        mockMvc.perform(get("/api/v1/admin/academies").session(session).param("q", "!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Academia Exclamacion!"));

        // La busqueda literal conserva la insensibilidad a mayusculas/minusculas.
        mockMvc.perform(get("/api/v1/admin/academies").session(session).param("q", "CIEN %"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Academia Cien %"));

        // La busqueda de texto normal sigue funcionando sin comodines.
        mockMvc.perform(get("/api/v1/admin/academies").session(session).param("q", "academia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4));

        mockMvc.perform(get("/api/v1/admin/clubs").session(session).param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Club Cien %"));

        mockMvc.perform(get("/api/v1/admin/clubs").session(session).param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Club Guion_Bajo"));

        mockMvc.perform(get("/api/v1/admin/clubs").session(session).param("q", "!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Club Exclamacion!"));
    }

    @Test
    void validationRejectsMissingFieldsInvalidUrlsRucYearAndUbigeo() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-validation@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);
        String ruc = nextRuc();

        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Academia Valida", ruc))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("RUC Duplicado", ruc))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RUC_ALREADY_EXISTS"));

        Map<String, Object> missingName = academyBody("", nextRuc());
        missingName.remove("name");
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(missingName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Map<String, Object> badLogo = academyBody("Logo Malo", nextRuc());
        badLogo.put("logoUrl", "ftp://example.com/logo.png");
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badLogo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Map<String, Object> badRuc = academyBody("RUC Malo", "123");
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badRuc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        Map<String, Object> futureYear = academyBody("Anio Futuro", nextRuc());
        futureYear.put("foundationYear", 2100);
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(futureYear)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FOUNDATION_YEAR"));

        Map<String, Object> badUbigeo = academyBody("Ubigeo Malo", nextRuc());
        badUbigeo.put("ubigeoCode", "999999");
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(badUbigeo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UBIGEO"));
    }

    @Test
    void malformedUbigeoFormatAndUnknownCodeReturnInvalidUbigeo() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-ubigeo-validation@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        for (String ubigeo : List.of("15A101", "15010", "1501010", " 150101", "150101 ", "999999")) {
            Map<String, Object> body = academyBody("Academia UBIGEO " + ubigeo, nextRuc());
            body.put("ubigeoCode", ubigeo);
            mockMvc.perform(post("/api/v1/admin/academies")
                            .session(session).header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_UBIGEO"));
        }

        Map<String, Object> clubBody = clubBody("Club UBIGEO Malo", nextRuc());
        clubBody.put("ubigeoCode", "15A101");
        mockMvc.perform(post("/api/v1/admin/clubs")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(clubBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UBIGEO"));

        // La misma regla aplica al reemplazo total.
        Academy academy = createAcademyEntity("Academia UBIGEO Update", "ACA-T-UBI-1",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Map<String, Object> update = academyBody("Academia UBIGEO Update", academy.getRuc());
        update.put("ubigeoCode", "15A101");
        mockMvc.perform(put("/api/v1/admin/academies/" + academy.getId())
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_UBIGEO"));
    }

    @Test
    void urlValidationRejectsHostlessNonHttpAbsoluteAndRelativeUrls() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-url-validation@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        List<String> invalidUrls = List.of(
                "https://#",
                "https://",
                "data:text/html,<script>alert(1)</script>",
                "javascript:alert(1)",
                "cdn.example.com/logo.png",
                "https://cdn.example.com/logo with space.png",
                "//cdn.example.com/logo.png"
        );

        for (String url : invalidUrls) {
            Map<String, Object> body = academyBody("Logo Invalido", nextRuc());
            body.put("logoUrl", url);
            mockMvc.perform(post("/api/v1/admin/academies")
                            .session(session).header("X-CSRF-TOKEN", csrf.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.fieldErrors.logoUrl").exists());
        }

        Map<String, Object> optionalUrl = academyBody("URL Opcional Invalida", nextRuc());
        optionalUrl.put("websiteUrl", "javascript:alert(1)");
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(optionalUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.websiteUrl").exists());

        Map<String, Object> invalidClubUrl = clubBody("Club Logo Invalido", nextRuc());
        invalidClubUrl.put("logoUrl", "https://#");
        mockMvc.perform(post("/api/v1/admin/clubs")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(invalidClubUrl)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.logoUrl").exists());

        // Una URI absoluta http/https con host sigue siendo valida.
        Map<String, Object> validUrl = academyBody("URL Valida", nextRuc());
        validUrl.put("logoUrl", "http://example.com/logo.png");
        mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(validUrl)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logoUrl").value("http://example.com/logo.png"));
    }

    @Test
    void realRucUniqueViolationIsTranslatedWithoutMaskingOtherConstraints() {
        String ruc = nextRuc();
        Academy first = createAcademyEntity("Academia RUC Unico", "ACA-T-RUC-1",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        first.setRuc(ruc);
        academyRepository.saveAndFlush(first);

        Academy duplicate = createAcademyEntity("Academia RUC Duplicado", "ACA-T-RUC-2",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        duplicate.setRuc(ruc);

        DataIntegrityViolationException ex = assertThrows(DataIntegrityViolationException.class,
                () -> academyRepository.saveAndFlush(duplicate));

        assertTrue(OrganizationDataIntegrity.isRucConflict(
                ex, OrganizationDataIntegrity.ACADEMY_RUC_CONSTRAINT));
        assertFalse(OrganizationDataIntegrity.isRucConflict(
                ex, OrganizationDataIntegrity.CLUB_RUC_CONSTRAINT));

        ApiException translated = OrganizationDataIntegrity.rucConflictOrNull(
                ex, OrganizationDataIntegrity.ACADEMY_RUC_CONSTRAINT,
                "El RUC ya esta registrado en otra academia.");
        assertNotNull(translated);
        assertEquals("RUC_ALREADY_EXISTS", translated.getCode());

        String clubRuc = nextRuc();
        Club firstClub = createClubEntity("Club RUC Unico", "CLU-T-RUC-1",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        firstClub.setRuc(clubRuc);
        clubRepository.saveAndFlush(firstClub);

        Club duplicateClub = createClubEntity("Club RUC Duplicado", "CLU-T-RUC-2",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        duplicateClub.setRuc(clubRuc);

        DataIntegrityViolationException clubEx = assertThrows(DataIntegrityViolationException.class,
                () -> clubRepository.saveAndFlush(duplicateClub));

        assertTrue(OrganizationDataIntegrity.isRucConflict(
                clubEx, OrganizationDataIntegrity.CLUB_RUC_CONSTRAINT));
        assertFalse(OrganizationDataIntegrity.isRucConflict(
                clubEx, OrganizationDataIntegrity.ACADEMY_RUC_CONSTRAINT));

        ApiException translatedClub = OrganizationDataIntegrity.rucConflictOrNull(
                clubEx, OrganizationDataIntegrity.CLUB_RUC_CONSTRAINT,
                "El RUC ya esta registrado en otro club.");
        assertNotNull(translatedClub);
        assertEquals("RUC_ALREADY_EXISTS", translatedClub.getCode());
    }

    @Test
    void concurrentCreateWithSameRucReturnsRucAlreadyExists() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-race@example.com");
        MockHttpSession sessionA = login(admin.getEmail());
        MockHttpSession sessionB = login(admin.getEmail());
        Csrf csrfA = csrf(sessionA);
        Csrf csrfB = csrf(sessionB);
        String ruc = nextRuc();
        String requestBody = json(academyBody("Academia Concurrente", ruc));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> first = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/v1/admin/academies")
                                .session(sessionA).header("X-CSRF-TOKEN", csrfA.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn();
            };
            Callable<MvcResult> second = () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return mockMvc.perform(post("/api/v1/admin/academies")
                                .session(sessionB).header("X-CSRF-TOKEN", csrfB.token())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                        .andReturn();
            };

            Future<MvcResult> firstResult = executor.submit(first);
            Future<MvcResult> secondResult = executor.submit(second);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<MvcResult> results = List.of(
                    firstResult.get(15, TimeUnit.SECONDS),
                    secondResult.get(15, TimeUnit.SECONDS));
            List<Integer> statuses = results.stream()
                    .map(result -> result.getResponse().getStatus())
                    .sorted()
                    .toList();
            assertEquals(List.of(201, 409), statuses);

            MvcResult conflict = results.stream()
                    .filter(result -> result.getResponse().getStatus() == 409)
                    .findFirst()
                    .orElseThrow();
            JsonNode error = objectMapper.readTree(conflict.getResponse().getContentAsString());
            assertEquals("RUC_ALREADY_EXISTS", error.get("code").asText());
            assertEquals(1, academyRepository.count());
        } finally {
            executor.shutdownNow();
        }
    }

    // ---------------------------------------------------------------------
    // Alcance propio y directorio
    // ---------------------------------------------------------------------

    @Test
    void directorReadsAndEditsOnlyOwnAcademyAndResetsVerification() throws Exception {
        Academy academy = createAcademyEntity("Academia Propia", "ACA-T-200",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User director = createUser(Role.ACADEMY_DIRECTOR, "director@example.com");
        associate(director, academy);
        MockHttpSession session = login(director.getEmail());
        Csrf csrf = csrf(session);

        mockMvc.perform(get("/api/v1/academies/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(academy.getId()))
                .andExpect(jsonPath("$.ruc").isNotEmpty())
                .andExpect(jsonPath("$.representativeName").value("Nombre Apellido"))
                .andExpect(jsonPath("$.counts.players").value(nullValue()));

        Map<String, Object> update = academyBody("Academia Propia Editada", academy.getRuc());
        update.put("type", "Tipo actualizado");
        mockMvc.perform(put("/api/v1/academies/me")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(academy.getId()))
                .andExpect(jsonPath("$.name").value("Academia Propia Editada"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"));

        // Un cambio sensible del Director reinicia la verificacion.
        assertEquals(VerificationStatus.PENDING_VERIFICATION,
                academyRepository.findById(academy.getId()).orElseThrow().getVerificationStatus());

        // El Director puede inhabilitar su propia academia.
        mockMvc.perform(patch("/api/v1/academies/me/deactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/academies/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("INACTIVE"));

        mockMvc.perform(put("/api/v1/academies/me")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_INACTIVE"));

        mockMvc.perform(patch("/api/v1/academies/me/deactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_ALREADY_INACTIVE"));

        // Solo ADMIN puede reactivar.
        mockMvc.perform(patch("/api/v1/admin/academies/" + academy.getId() + "/reactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isForbidden());

        User admin = createUser(Role.ADMIN, "admin-reactivate@example.com");
        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);
        mockMvc.perform(patch("/api/v1/admin/academies/" + academy.getId() + "/reactivate")
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/academies/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("ACTIVE"));
    }

    @Test
    void professorReadsOwnAcademyButCannotEditAndPlayerHasNoAccess() throws Exception {
        Academy academy = createAcademyEntity("Academia Docentes", "ACA-T-201",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User professor = createUser(Role.PROFESSOR, "professor@example.com");
        associate(professor, academy);
        MockHttpSession professorSession = login(professor.getEmail());
        Csrf professorCsrf = csrf(professorSession);

        mockMvc.perform(get("/api/v1/academies/me").session(professorSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(academy.getId()));
        mockMvc.perform(put("/api/v1/academies/me")
                        .session(professorSession).header("X-CSRF-TOKEN", professorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Intento", academy.getRuc()))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(patch("/api/v1/academies/me/deactivate")
                        .session(professorSession).header("X-CSRF-TOKEN", professorCsrf.token()))
                .andExpect(status().isForbidden());

        User player = createUser(Role.PLAYER, "player@example.com");
        associate(player, academy);
        MockHttpSession playerSession = login(player.getEmail());
        mockMvc.perform(get("/api/v1/academies/me").session(playerSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/clubs/me").session(playerSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/ubigeo/departments").session(playerSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void directorCannotUseClubScopeOrAdminPathsAndManagerCannotUseAcademyScope() throws Exception {
        Academy academy = createAcademyEntity("Academia Cruzada", "ACA-T-202",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club club = createClubEntity("Club Cruzado", "CLU-T-202",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User director = createUser(Role.ACADEMY_DIRECTOR, "director-cross@example.com");
        associate(director, academy);
        User manager = createUser(Role.CLUB_MANAGER, "manager-cross@example.com");
        associate(manager, club);

        MockHttpSession directorSession = login(director.getEmail());
        mockMvc.perform(get("/api/v1/clubs/me").session(directorSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/academies/" + academy.getId()).session(directorSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/directory/academies").session(directorSession))
                .andExpect(status().isForbidden());

        MockHttpSession managerSession = login(manager.getEmail());
        mockMvc.perform(get("/api/v1/academies/me").session(managerSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/academies/me").session(managerSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void clubManagerReadsAndEditsOwnClubWithFixedCountryAndNoDirectoryPii() throws Exception {
        Club club = createClubEntity("Club Propio", "CLU-T-300",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        User manager = createUser(Role.CLUB_MANAGER, "manager@example.com");
        associate(manager, club);
        MockHttpSession session = login(manager.getEmail());
        Csrf csrf = csrf(session);

        mockMvc.perform(get("/api/v1/clubs/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(club.getId()))
                .andExpect(jsonPath("$.countryCode").value("PE"))
                .andExpect(jsonPath("$.responsibleNames[0]").value("Nombre Apellido"))
                .andExpect(jsonPath("$.representativeName").doesNotExist())
                .andExpect(jsonPath("$.counts.players").value(nullValue()));

        Map<String, Object> update = clubBody("Club Propio Editado", club.getRuc());
        update.put("countryCode", "US");
        mockMvc.perform(put("/api/v1/clubs/me")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("PE"))
                .andExpect(jsonPath("$.verificationStatus").value("PENDING_VERIFICATION"));

        // El Responsable puede inhabilitar su propio club.
        mockMvc.perform(patch("/api/v1/clubs/me/deactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/clubs/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("INACTIVE"));
        mockMvc.perform(put("/api/v1/clubs/me")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_INACTIVE"));
        mockMvc.perform(patch("/api/v1/clubs/me/deactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ORGANIZATION_ALREADY_INACTIVE"));

        // Solo ADMIN puede reactivar; el Responsable no usa rutas administrativas.
        mockMvc.perform(patch("/api/v1/admin/clubs/" + club.getId() + "/reactivate")
                        .session(session).header("X-CSRF-TOKEN", csrf.token()))
                .andExpect(status().isForbidden());

        User admin = createUser(Role.ADMIN, "admin-club-reactivate@example.com");
        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);
        mockMvc.perform(patch("/api/v1/admin/clubs/" + club.getId() + "/reactivate")
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("ACTIVE"));
        mockMvc.perform(get("/api/v1/clubs/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationalStatus").value("ACTIVE"));
    }

    @Test
    void scoutDirectoryOnlyReturnsActiveVerifiedAndOmitsRepresentationPii() throws Exception {
        Academy visible = createAcademyEntity("Academia Visible", "ACA-T-400",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Academy pending = createAcademyEntity("Academia Pendiente", "ACA-T-401",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);
        Academy inactive = createAcademyEntity("Academia Inactiva", "ACA-T-402",
                OperationalStatus.INACTIVE, VerificationStatus.VERIFIED);
        createAcademyEntity("Academia Rechazada", "ACA-T-403",
                OperationalStatus.ACTIVE, VerificationStatus.REJECTED);

        Club visibleClub = createClubEntity("Club Visible", "CLU-T-400",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createClubEntity("Club Pendiente", "CLU-T-401",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);

        User scout = createUser(Role.SCOUT, "scout-directory@example.com");
        MockHttpSession session = login(scout.getEmail());

        MvcResult academyList = mockMvc.perform(get("/api/v1/directory/academies").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(visible.getId()))
                .andExpect(jsonPath("$.content[0].ruc").doesNotExist())
                .andExpect(jsonPath("$.content[0].representativeName").doesNotExist())
                .andExpect(jsonPath("$.content[0].counts.players").value(nullValue()))
                .andReturn();
        String academyJson = academyList.getResponse().getContentAsString();
        assertFalse(academyJson.contains("RUC"));
        assertFalse(academyJson.contains("representativeName"));

        // Parametros de estado no declarados no amplian el resultado forzado.
        mockMvc.perform(get("/api/v1/directory/academies")
                        .session(session)
                        .param("operationalStatus", "INACTIVE")
                        .param("verificationStatus", "PENDING_VERIFICATION")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(visible.getId()));

        mockMvc.perform(get("/api/v1/directory/academies/" + visible.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").doesNotExist());
        mockMvc.perform(get("/api/v1/directory/academies")
                        .session(session).param("q", "visible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/directory/academies")
                        .session(session).param("type", "otro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/directory/academies/" + pending.getId()).session(session))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/directory/academies/" + inactive.getId()).session(session))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/directory/clubs").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(visibleClub.getId()))
                .andExpect(jsonPath("$.content[0].ruc").doesNotExist())
                .andExpect(jsonPath("$.content[0].responsibleNames").doesNotExist());
        mockMvc.perform(get("/api/v1/directory/clubs/" + visibleClub.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsibleNames").doesNotExist());

        // Otros roles no acceden al directorio.
        User player = createUser(Role.PLAYER, "player-directory@example.com");
        MockHttpSession playerSession = login(player.getEmail());
        mockMvc.perform(get("/api/v1/directory/academies").session(playerSession))
                .andExpect(status().isForbidden());
    }

    @Test
    void directorySearchTreatsLikeWildcardsAsLiteralCharactersAndKeepsForcedStatuses() throws Exception {
        createAcademyEntity("Academia Cien %", "ACA-T-610",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createAcademyEntity("Academia Guion_Bajo", "ACA-T-611",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createAcademyEntity("Academia Normal", "ACA-T-612",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        // No debe aparecer: el estado forzado sigue aplicandose aunque el nombre
        // contenga el caracter buscado.
        createAcademyEntity("Academia Pendiente %", "ACA-T-613",
                OperationalStatus.ACTIVE, VerificationStatus.PENDING_VERIFICATION);

        createClubEntity("Club Cien %", "CLU-T-610",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createClubEntity("Club Guion_Bajo", "CLU-T-611",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createClubEntity("Club Normal", "CLU-T-612",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        createClubEntity("Club Rechazado _", "CLU-T-613",
                OperationalStatus.ACTIVE, VerificationStatus.REJECTED);

        User scout = createUser(Role.SCOUT, "scout-literal-search@example.com");
        MockHttpSession session = login(scout.getEmail());

        mockMvc.perform(get("/api/v1/directory/academies").session(session).param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Academia Cien %"));

        mockMvc.perform(get("/api/v1/directory/academies").session(session).param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Academia Guion_Bajo"));

        mockMvc.perform(get("/api/v1/directory/academies").session(session).param("q", "cien %"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Academia Cien %"));

        mockMvc.perform(get("/api/v1/directory/academies").session(session).param("q", "academia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/directory/clubs").session(session).param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Club Cien %"));

        mockMvc.perform(get("/api/v1/directory/clubs").session(session).param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Club Guion_Bajo"));
    }

    @Test
    void ubigeoEndpointsUseLocalCatalogAndReturnValidatedErrors() throws Exception {
        User scout = createUser(Role.SCOUT, "scout-ubigeo@example.com");
        MockHttpSession session = login(scout.getEmail());

        mockMvc.perform(get("/api/v1/ubigeo/departments").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(25))
                .andExpect(jsonPath("$[0].code").value("01"))
                .andExpect(jsonPath("$[0].name").value("Amazonas"));

        mockMvc.perform(get("/api/v1/ubigeo/provinces")
                        .session(session).param("departmentCode", "01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].code").value("0101"));

        mockMvc.perform(get("/api/v1/ubigeo/districts")
                        .session(session).param("provinceCode", "0101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(21))
                .andExpect(jsonPath("$[0].code").value("010101"));

        mockMvc.perform(get("/api/v1/ubigeo/provinces")
                        .session(session).param("departmentCode", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DEPARTMENT_CODE"));
        mockMvc.perform(get("/api/v1/ubigeo/provinces")
                        .session(session).param("departmentCode", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UBIGEO_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/ubigeo/provinces").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mockMvc.perform(get("/api/v1/ubigeo/districts")
                        .session(session).param("provinceCode", "999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PROVINCE_CODE"));
        mockMvc.perform(get("/api/v1/ubigeo/districts")
                        .session(session).param("provinceCode", "9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UBIGEO_NOT_FOUND"));
    }

    @Test
    void userOrganizationRequiresOneTargetAndCompatibleRole() {
        Academy academy = createAcademyEntity("Academia Asociacion", "ACA-T-500",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);
        Club club = createClubEntity("Club Asociacion", "CLU-T-500",
                OperationalStatus.ACTIVE, VerificationStatus.VERIFIED);

        User admin = createUser(Role.ADMIN, "admin-association@example.com");
        User scout = createUser(Role.SCOUT, "scout-association@example.com");
        User director = createUser(Role.ACADEMY_DIRECTOR, "director-association@example.com");
        User manager = createUser(Role.CLUB_MANAGER, "manager-association@example.com");
        User player = createUser(Role.PLAYER, "player-association@example.com");

        assertThrows(RuntimeException.class, () -> saveAssociation(admin, academy, null));
        assertThrows(RuntimeException.class, () -> saveAssociation(scout, academy, null));
        assertThrows(RuntimeException.class, () -> saveAssociation(director, null, club));
        assertThrows(RuntimeException.class, () -> saveAssociation(manager, academy, null));

        UserOrganization playerAssociation = saveAssociation(player, academy, null);
        assertNotNull(playerAssociation.getId());
        assertEquals(academy.getId(), userOrganizationRepository.findByUser_Id(player.getId())
                .orElseThrow().getAcademy().getId());

        User professor = createUser(Role.PROFESSOR, "professor-association@example.com");
        saveAssociation(professor, academy, null);
        assertThrows(DataIntegrityViolationException.class,
                () -> saveAssociation(professor, academy, null));
    }

    @Test
    void responseNeverSerializesJpaEntityInternals() throws Exception {
        User admin = createUser(Role.ADMIN, "admin-serialization@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        MvcResult result = mockMvc.perform(post("/api/v1/admin/academies")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(academyBody("Academia Serializada", nextRuc()))))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("passwordHash"));
        assertFalse(body.contains("hibernateLazyInitializer"));
        assertFalse(body.contains("userOrganizations"));
        assertFalse(body.contains("Handler"));
        assertFalse(body.contains("target"));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void verify(MockHttpSession session, Csrf csrf, Long id, String statusValue, int expectedStatus)
            throws Exception {
        mockMvc.perform(patch("/api/v1/admin/academies/" + id + "/verification")
                        .session(session)
                        .header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + statusValue + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    private User createUser(Role role, String email) {
        return createUser(role, email, "Nombre", "Apellido");
    }

    private User createUser(Role role, String email, String firstName, String lastName) {
        int n = sequence.incrementAndGet();
        User user = new User();
        user.setEmail(email.toLowerCase());
        user.setUsername("user" + n);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setDocumentType(DocumentType.DNI);
        user.setDocumentNumber("DOC" + n);
        user.setPhone("999999999");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setMustChangePassword(false);
        return userRepository.saveAndFlush(user);
    }

    private Academy createAcademyEntity(String name, String code,
                                        OperationalStatus operationalStatus,
                                        VerificationStatus verificationStatus) {
        Academy academy = new Academy();
        academy.setCode(code);
        academy.setName(name);
        academy.setLogoUrl("https://cdn.example.com/academy.png");
        academy.setType("Academia de futbol");
        academy.setDescription("Descripcion institucional.");
        academy.setFoundationYear(2001);
        academy.setRuc(nextRuc());
        academy.setUbigeoCode(VALID_UBIGEO);
        academy.setAddress("Av. Institucional 123");
        academy.setInstitutionalEmail("contacto@academia.example.com");
        academy.setInstitutionalPhone("+51 1 555 0101");
        academy.setWebsiteUrl("https://academia.example.com");
        academy.setInstagramUrl("https://instagram.com/academia");
        academy.setFacebookUrl("https://facebook.com/academia");
        academy.setYoutubeUrl("https://youtube.com/@academia");
        academy.setTiktokUrl("https://tiktok.com/@academia");
        academy.setTrainingGroups(new ArrayList<>(List.of("Sub 12")));
        academy.setHeadquarters("Sede principal");
        academy.setRepresentativeName("Ana Torres");
        academy.setOperationalStatus(operationalStatus);
        academy.setVerificationStatus(verificationStatus);
        return academyRepository.saveAndFlush(academy);
    }

    private Club createClubEntity(String name, String code,
                                  OperationalStatus operationalStatus,
                                  VerificationStatus verificationStatus) {
        Club club = new Club();
        club.setCode(code);
        club.setName(name);
        club.setType("Club de futbol");
        club.setRuc(nextRuc());
        club.setFoundationYear(1998);
        club.setLogoUrl("https://cdn.example.com/club.png");
        club.setDescription("Descripcion institucional del club.");
        club.setCountryCode("PE");
        club.setUbigeoCode(VALID_UBIGEO);
        club.setAddress("Av. Institucional 456");
        club.setInstitutionalEmail("contacto@club.example.com");
        club.setInstitutionalPhone("+51 1 555 0202");
        club.setWebsiteUrl("https://club.example.com");
        club.setInstagramUrl("https://instagram.com/club");
        club.setFacebookUrl("https://facebook.com/club");
        club.setYoutubeUrl("https://youtube.com/@club");
        club.setTiktokUrl("https://tiktok.com/@club");
        club.setOperationalStatus(operationalStatus);
        club.setVerificationStatus(verificationStatus);
        return clubRepository.saveAndFlush(club);
    }

    private UserOrganization associate(User user, Academy academy) {
        return saveAssociation(user, academy, null);
    }

    private UserOrganization associate(User user, Club club) {
        return saveAssociation(user, null, club);
    }

    private UserOrganization saveAssociation(User user, Academy academy, Club club) {
        UserOrganization association = new UserOrganization();
        association.setUser(user);
        association.setAcademy(academy);
        association.setClub(club);
        return userOrganizationRepository.saveAndFlush(association);
    }

    private Map<String, Object> academyBody(String name, String ruc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("logoUrl", "https://cdn.example.com/logo.png");
        body.put("type", "Academia de futbol");
        body.put("description", "Descripcion institucional.");
        body.put("foundationYear", 2001);
        body.put("ruc", ruc);
        body.put("ubigeoCode", VALID_UBIGEO);
        body.put("address", "Av. Institucional 123");
        body.put("institutionalEmail", "contacto@academia.example.com");
        body.put("institutionalPhone", "+51 1 555 0101");
        body.put("websiteUrl", "https://academia.example.com");
        body.put("instagramUrl", "https://instagram.com/academia");
        body.put("facebookUrl", "https://facebook.com/academia");
        body.put("youtubeUrl", "https://youtube.com/@academia");
        body.put("tiktokUrl", "https://tiktok.com/@academia");
        body.put("trainingGroups", List.of("Sub 12", "Sub 15"));
        body.put("headquarters", "Sede principal");
        body.put("representativeName", "Ana Torres");
        return body;
    }

    private Map<String, Object> clubBody(String name, String ruc) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("type", "Club de futbol");
        body.put("ruc", ruc);
        body.put("foundationYear", 1998);
        body.put("logoUrl", "https://cdn.example.com/club.png");
        body.put("description", "Descripcion institucional del club.");
        body.put("ubigeoCode", VALID_UBIGEO);
        body.put("address", "Av. Institucional 456");
        body.put("institutionalEmail", "contacto@club.example.com");
        body.put("institutionalPhone", "+51 1 555 0202");
        body.put("websiteUrl", "https://club.example.com");
        body.put("instagramUrl", "https://instagram.com/club");
        body.put("facebookUrl", "https://facebook.com/club");
        body.put("youtubeUrl", "https://youtube.com/@club");
        body.put("tiktokUrl", "https://tiktok.com/@club");
        return body;
    }

    private String nextRuc() {
        return String.format("20%09d", sequence.incrementAndGet());
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
