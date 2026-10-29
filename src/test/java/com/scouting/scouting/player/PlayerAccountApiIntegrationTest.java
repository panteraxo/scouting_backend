package com.scouting.scouting.player;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.entity.VerificationStatus;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.entity.PlayerProfile;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SCP-015: el alta desde Jugadores crea cuenta PLAYER y perfil en una sola
 * transaccion. La creacion general de usuarios ya no admite PLAYER.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scouting_player_accounts;MODE=MySQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlayerAccountApiIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    private final AtomicInteger sequence = new AtomicInteger(1);

    @BeforeEach
    void cleanDatabase() {
        playerProfileRepository.deleteAll();
        userOrganizationRepository.deleteAll();
        academyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void adminCreatesActivePlayerAccountAndProfileInOneRequest() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-account@example.com");
        Academy academy = academy("Academia Alta");

        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);
        Map<String, Object> body = body("nuevo.jugador@example.com", academy.getId(), null);

        MvcResult result = mockMvc.perform(post("/api/v1/players/with-account")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.player.id").isNumber())
                .andExpect(jsonPath("$.player.userId").isNumber())
                .andExpect(jsonPath("$.player.profileStatus").value("COMPLETE"))
                .andExpect(jsonPath("$.player.academy.id").value(academy.getId()))
                .andExpect(jsonPath("$.temporaryPassword").isNotEmpty())
                .andExpect(jsonPath("$.temporaryPasswordExpiresAt").isNotEmpty())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        long userId = response.get("player").get("userId").asLong();
        String temporaryPassword = response.get("temporaryPassword").asText();

        User stored = userRepository.findById(userId).orElseThrow();
        assertEquals(Role.PLAYER, stored.getRole());
        assertEquals(UserStatus.ACTIVE, stored.getStatus());
        assertEquals("nuevo.jugador@example.com", stored.getEmail());
        assertTrue(stored.isMustChangePassword());
        assertNotNull(stored.getTemporaryPasswordExpiresAt());
        assertNotEquals(temporaryPassword, stored.getPasswordHash());
        assertTrue(passwordEncoder.matches(temporaryPassword, stored.getPasswordHash()));

        UserOrganization organization = userOrganizationRepository.findByUser_Id(userId).orElseThrow();
        assertEquals(academy.getId(), organization.getAcademy().getId());
        assertNull(organization.getClub());

        PlayerProfile profile = playerProfileRepository.findByUser_Id(userId).orElseThrow();
        assertEquals(academy.getId(), userOrganizationRepository.findByUser_Id(userId)
                .orElseThrow().getAcademy().getId());
        assertNotNull(profile.getBirthDate());
        assertFalse(response.has("user"));
    }

    @Test
    void directorAndProfessorCreateAccountAndProfileInTheirOwnAcademy() throws Exception {
        Academy own = academy("Academia Propia");
        Academy other = academy("Academia Ajena");
        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-account@example.com");
        associate(director, own);
        User professor = user(Role.PROFESSOR, UserStatus.ACTIVE, "professor-account@example.com");
        associate(professor, own);

        MockHttpSession directorSession = login(director.getEmail());
        Csrf directorCsrf = csrf(directorSession);
        // El academyId enviado por staff se ignora y se usa el de la sesion.
        Map<String, Object> directorBody = body("director-creado@example.com", other.getId(), null);
        mockMvc.perform(post("/api/v1/players/with-account")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(directorBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.player.academy.id").value(own.getId()))
                .andExpect(jsonPath("$.player.guardianDataStatus").exists());
        User directorPlayer = userRepository.findByEmail("director-creado@example.com").orElseThrow();
        assertEquals(own.getId(), userOrganizationRepository.findByUser_Id(directorPlayer.getId())
                .orElseThrow().getAcademy().getId());
        assertTrue(playerProfileRepository.existsByUser_Id(directorPlayer.getId()));

        MockHttpSession professorSession = login(professor.getEmail());
        Csrf professorCsrf = csrf(professorSession);
        Map<String, Object> professorBody = body("professor-creado@example.com", null, null);
        mockMvc.perform(post("/api/v1/players/with-account")
                        .session(professorSession).header("X-CSRF-TOKEN", professorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(professorBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.player.academy.id").value(own.getId()));
        User professorPlayer = userRepository.findByEmail("professor-creado@example.com").orElseThrow();
        assertEquals(own.getId(), userOrganizationRepository.findByUser_Id(professorPlayer.getId())
                .orElseThrow().getAcademy().getId());
        assertTrue(playerProfileRepository.existsByUser_Id(professorPlayer.getId()));
    }

    @Test
    void adminWithoutAcademyIsRejectedWithoutCreatingAnything() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-no-academy@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> body = body("sin-academia@example.com", null, null);
        mockMvc.perform(post("/api/v1/players/with-account")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ACADEMY_REQUIRED"));

        assertTrue(userRepository.findByEmail("sin-academia@example.com").isEmpty());
        assertEquals(0, playerProfileRepository.count());
    }

    @Test
    void duplicateIdentityIsRejectedWithoutCreatingProfile() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-duplicate-player@example.com");
        Academy academy = academy("Academia Duplicados");
        User existing = user(Role.SCOUT, UserStatus.ACTIVE, "existente@example.com");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> duplicateEmail = body(existing.getEmail(), academy.getId(), null);
        assertConflictWithoutProfile(session, csrf, duplicateEmail);

        Map<String, Object> duplicateUsername = body("usuario-duplicado@example.com", academy.getId(), null);
        duplicateUsername.put("username", existing.getUsername());
        assertConflictWithoutProfile(session, csrf, duplicateUsername);

        Map<String, Object> duplicateDocument = body("documento-duplicado@example.com", academy.getId(), null);
        duplicateDocument.put("documentNumber", existing.getDocumentNumber());
        assertConflictWithoutProfile(session, csrf, duplicateDocument);
    }

    @Test
    void invalidProfileRollsBackThePlayerAccount() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-rollback@example.com");
        Academy academy = academy("Academia Rollback");
        MockHttpSession session = login(admin.getEmail());
        Csrf csrf = csrf(session);

        Map<String, Object> body = body("rollback@example.com", academy.getId(), null);
        body.put("birthDate", PlayerTime.today().plusDays(1).toString());

        mockMvc.perform(post("/api/v1/players/with-account")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BIRTH_DATE"));

        assertTrue(userRepository.findByEmail("rollback@example.com").isEmpty());
        assertEquals(0, playerProfileRepository.count());
        assertEquals(1, userRepository.count());
    }

    @Test
    void generalUserCreationRejectsPlayerRole() throws Exception {
        User admin = user(Role.ADMIN, UserStatus.ACTIVE, "admin-general@example.com");
        Academy academy = academy("Academia General");
        MockHttpSession adminSession = login(admin.getEmail());
        Csrf adminCsrf = csrf(adminSession);

        Map<String, Object> adminBody = userBody("player-admin-endpoint@example.com", "PLAYER");
        adminBody.put("academyId", academy.getId());
        mockMvc.perform(post("/api/v1/admin/users")
                        .session(adminSession).header("X-CSRF-TOKEN", adminCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(adminBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROLE"));
        assertTrue(userRepository.findByEmail("player-admin-endpoint@example.com").isEmpty());

        User director = user(Role.ACADEMY_DIRECTOR, UserStatus.ACTIVE, "director-general@example.com");
        associate(director, academy);
        MockHttpSession directorSession = login(director.getEmail());
        Csrf directorCsrf = csrf(directorSession);

        mockMvc.perform(post("/api/v1/academies/me/users")
                        .session(directorSession).header("X-CSRF-TOKEN", directorCsrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(userBody("player-scoped-endpoint@example.com", "PLAYER"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_NOT_ALLOWED"));
        assertTrue(userRepository.findByEmail("player-scoped-endpoint@example.com").isEmpty());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void assertConflictWithoutProfile(MockHttpSession session, Csrf csrf,
                                              Map<String, Object> body) throws Exception {
        mockMvc.perform(post("/api/v1/players/with-account")
                        .session(session).header("X-CSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));
        assertEquals(0, playerProfileRepository.count());
        assertEquals(2, userRepository.count());
    }

    private Map<String, Object> body(String email, Long academyId, String phone) {
        int n = sequence.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", "Luis");
        body.put("lastName", "Quispe");
        body.put("documentType", "DNI");
        body.put("documentNumber", "PLY" + n);
        body.put("phone", phone);
        body.put("email", email);
        body.put("username", "jugador" + n);
        body.put("academyId", academyId);
        body.put("birthDate", PlayerTime.today().minusYears(20).toString());
        body.put("residenceUbigeoCode", LIMA);
        body.put("heightCm", 172);
        body.put("weightKg", 64.50);
        body.put("position", "ATTACKING_MIDFIELDER");
        body.put("bestPosition", "LEFT_WINGER");
        body.put("dominantFoot", "RIGHT");
        body.put("summary", "Resumen del jugador.");
        body.put("photoUrl", "https://example.com/player.jpg");
        return body;
    }

    private Map<String, Object> userBody(String email, String role) {
        int n = sequence.incrementAndGet();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("firstName", "Nombre" + n);
        body.put("lastName", "Apellido" + n);
        body.put("documentType", "DNI");
        body.put("documentNumber", "USR" + n);
        body.put("phone", "999888777");
        body.put("email", email);
        body.put("username", "usuario" + n);
        body.put("role", role);
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

    private Academy academy(String name) {
        int n = sequence.incrementAndGet();
        Academy academy = new Academy();
        academy.setCode("ACA-PLYACC-" + n);
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
        academy.setOperationalStatus(OperationalStatus.ACTIVE);
        academy.setVerificationStatus(VerificationStatus.PENDING_VERIFICATION);
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
