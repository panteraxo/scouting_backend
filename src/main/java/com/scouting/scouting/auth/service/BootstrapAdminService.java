package com.scouting.scouting.auth.service;

import com.scouting.scouting.config.BootstrapAdminProperties;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Crea la primera cuenta ADMIN a partir de variables de entorno locales.
 *
 * <p>Es idempotente en ejecuciones secuenciales: si ya existe cualquier
 * administrador no modifica nada. No coordina instancias concurrentes, por lo que
 * el bootstrap debe ejecutarse en una unica instancia/proceso. La contrasena se
 * guarda hasheada, es temporal (24 horas) y obliga al cambio en el primer acceso.
 * No hay credenciales por defecto en el codigo ni en Git.</p>
 */
@Service
public class BootstrapAdminService {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminService.class);

    static final Duration TEMPORARY_PASSWORD_TTL = Duration.ofHours(24);
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;

    private final BootstrapAdminProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapAdminService(BootstrapAdminProperties properties,
                                 UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Asegura la existencia del administrador inicial. Puede invocarse varias veces
     * sin efectos adicionales.
     */
    @Transactional
    public void ensureAdmin() {
        if (!properties.isEnabled()) {
            return;
        }

        String email = blankToNull(properties.getEmail());
        String password = blankToNull(properties.getPassword());
        if (email == null && password == null) {
            return;
        }
        if (email == null || password == null) {
            throw new IllegalStateException(
                    "Bootstrap de administrador incompleto: configure BOOTSTRAP_ADMIN_EMAIL y "
                            + "BOOTSTRAP_ADMIN_PASSWORD, o deje ambos vacios.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD debe tener entre 8 y 72 caracteres.");
        }

        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Bootstrap de administrador omitido: ya existe una cuenta ADMIN.");
            return;
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Bootstrap de administrador omitido: el correo configurado ya pertenece a otra cuenta.");
            return;
        }

        String firstName = required(properties.getFirstName(), "BOOTSTRAP_ADMIN_FIRST_NAME");
        String lastName = required(properties.getLastName(), "BOOTSTRAP_ADMIN_LAST_NAME");
        String username = required(properties.getUsername(), "BOOTSTRAP_ADMIN_USERNAME").toLowerCase();
        if (userRepository.existsByUsername(username)) {
            log.warn("Bootstrap de administrador omitido: el nombre de usuario configurado ya existe.");
            return;
        }

        String documentNumber = blankToNull(properties.getDocumentNumber());
        if (documentNumber != null) {
            documentNumber = documentNumber.trim().toUpperCase();
            if (userRepository.existsByDocumentNumber(documentNumber)) {
                log.warn("Bootstrap de administrador omitido: el documento configurado ya existe.");
                return;
            }
        }

        Instant now = Instant.now();
        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setUsername(username);
        admin.setFirstName(firstName.trim());
        admin.setLastName(lastName.trim());
        admin.setPhone(blankToNull(properties.getPhone()));
        admin.setDocumentType(properties.getDocumentType());
        admin.setDocumentNumber(documentNumber);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setMustChangePassword(true);
        admin.setTemporaryPasswordExpiresAt(now.plus(TEMPORARY_PASSWORD_TTL));

        User saved = userRepository.save(admin);
        log.info("Cuenta ADMIN inicial creada (id={}) con cambio de contrasena obligatorio.", saved.getId());
    }

    private String required(String value, String variableName) {
        String trimmed = blankToNull(value);
        if (trimmed == null) {
            throw new IllegalStateException("Falta la variable " + variableName + " para crear el administrador inicial.");
        }
        return trimmed;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
