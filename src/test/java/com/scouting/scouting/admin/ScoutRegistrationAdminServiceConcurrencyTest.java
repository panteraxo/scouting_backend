package com.scouting.scouting.admin;

import com.scouting.scouting.admin.dto.ScoutRegistrationResponse;
import com.scouting.scouting.admin.service.ScoutRegistrationAdminService;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica que la transicion PENDING_APPROVAL -> ACTIVE/INACTIVE sea atomica:
 * dos administradores no pueden procesar la misma solicitud.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:scouting_admin_concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
})
@ActiveProfiles("test")
class ScoutRegistrationAdminServiceConcurrencyTest {

    @Autowired
    private ScoutRegistrationAdminService adminService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void onlyOneConcurrentOperationProcessesThePendingScout() throws Exception {
        User scout = createPendingScout();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        try {
            Future<Outcome> approve = executor.submit(attempt(barrier, () -> adminService.approve(scout.getId())));
            Future<Outcome> reject = executor.submit(attempt(barrier, () -> adminService.reject(scout.getId())));

            List<Outcome> outcomes = List.of(
                    approve.get(15, TimeUnit.SECONDS),
                    reject.get(15, TimeUnit.SECONDS));

            assertEquals(1, outcomes.stream().filter(Outcome::success).count(),
                    "Solo una operacion debe procesar la solicitud pendiente");
            Outcome rejected = outcomes.stream()
                    .filter(outcome -> !outcome.success())
                    .findFirst()
                    .orElseThrow();
            assertEquals("SCOUT_REGISTRATION_ALREADY_PROCESSED", rejected.errorCode());

            UserStatus expectedStatus = outcomes.get(0).success() ? UserStatus.ACTIVE : UserStatus.INACTIVE;
            User stored = userRepository.findById(scout.getId()).orElseThrow();
            assertEquals(expectedStatus, stored.getStatus());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void processedScoutCannotBeApprovedAgain() {
        User scout = createPendingScout();
        adminService.approve(scout.getId());

        ApiException conflict = assertThrows(
                ApiException.class, () -> adminService.approve(scout.getId()));

        assertEquals("SCOUT_REGISTRATION_ALREADY_PROCESSED", conflict.getCode());
        assertEquals(HttpStatus.CONFLICT, conflict.getStatus());
        assertEquals(UserStatus.ACTIVE, userRepository.findById(scout.getId()).orElseThrow().getStatus());
    }

    private Callable<Outcome> attempt(CyclicBarrier barrier, Operation operation) {
        return () -> {
            barrier.await(5, TimeUnit.SECONDS);
            try {
                ScoutRegistrationResponse response = operation.run();
                return new Outcome(true, null, response.status());
            } catch (ApiException exception) {
                return new Outcome(false, exception.getCode(), null);
            }
        };
    }

    private User createPendingScout() {
        User scout = new User();
        scout.setEmail("concurrent@example.com");
        scout.setUsername("concurrentscout");
        scout.setFirstName("Nombre");
        scout.setLastName("Apellido");
        scout.setDocumentType(DocumentType.DNI);
        scout.setDocumentNumber("70000001");
        scout.setPhone("999999999");
        scout.setPasswordHash(passwordEncoder.encode("Password123!"));
        scout.setRole(Role.SCOUT);
        scout.setStatus(UserStatus.PENDING_APPROVAL);
        scout.setMustChangePassword(false);
        return userRepository.saveAndFlush(scout);
    }

    @FunctionalInterface
    private interface Operation {
        ScoutRegistrationResponse run();
    }

    private record Outcome(boolean success, String errorCode, UserStatus status) {
    }
}
