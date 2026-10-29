package com.scouting.scouting.auth;

import com.scouting.scouting.auth.service.BootstrapAdminService;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scouting_bootstrap;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "bootstrap.admin.enabled=true",
        "bootstrap.admin.email=root@example.com",
        "bootstrap.admin.password=RootPassword123!",
        "bootstrap.admin.first-name=Root",
        "bootstrap.admin.last-name=Admin",
        "bootstrap.admin.username=rootadmin"
})
@ActiveProfiles("test")
class BootstrapAdminServiceTest {

    @Autowired
    private BootstrapAdminService bootstrapAdminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void bootstrapCreatesHashedTemporaryAdminOnceAndNeverOverwrites() {
        List<User> admins = admins();
        assertEquals(1, admins.size());

        User admin = admins.get(0);
        assertEquals("root@example.com", admin.getEmail());
        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isMustChangePassword());
        assertNotNull(admin.getTemporaryPasswordExpiresAt());
        assertNotEquals("RootPassword123!", admin.getPasswordHash());
        assertTrue(passwordEncoder.matches("RootPassword123!", admin.getPasswordHash()));

        Instant expectedMaxExpiry = Instant.now().plus(Duration.ofHours(24)).plusSeconds(60);
        Instant expectedMinExpiry = Instant.now().plus(Duration.ofHours(23));
        assertTrue(admin.getTemporaryPasswordExpiresAt().isBefore(expectedMaxExpiry));
        assertTrue(admin.getTemporaryPasswordExpiresAt().isAfter(expectedMinExpiry));

        String originalHash = admin.getPasswordHash();
        Instant originalExpiry = admin.getTemporaryPasswordExpiresAt();

        bootstrapAdminService.ensureAdmin();
        bootstrapAdminService.ensureAdmin();

        List<User> adminsAfter = admins();
        assertEquals(1, adminsAfter.size());
        assertEquals(originalHash, adminsAfter.get(0).getPasswordHash());
        assertEquals(originalExpiry, adminsAfter.get(0).getTemporaryPasswordExpiresAt());

        // Un administrador existente (aunque cambie su hash) no es sobrescrito.
        User existing = adminsAfter.get(0);
        existing.setPasswordHash(passwordEncoder.encode("OtroHashDiferente123!"));
        existing.setMustChangePassword(false);
        existing.setTemporaryPasswordExpiresAt(null);
        userRepository.saveAndFlush(existing);

        bootstrapAdminService.ensureAdmin();

        User reloaded = userRepository.findById(existing.getId()).orElseThrow();
        assertEquals(existing.getPasswordHash(), reloaded.getPasswordHash());
        assertFalse(reloaded.isMustChangePassword());
        assertEquals(null, reloaded.getTemporaryPasswordExpiresAt());
        assertEquals(1, admins().size());
    }

    private List<User> admins() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .toList();
    }
}
