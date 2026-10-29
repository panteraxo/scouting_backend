package com.scouting.scouting.auth.service;

import com.scouting.scouting.auth.dto.RegisterScoutRequest;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registro publico de ojeadores.
 *
 * <p>El rol y el estado se fijan en el servidor ({@code SCOUT} /
 * {@code PENDING_APPROVAL}). El registro no autentica ni crea sesion. Cuando ya
 * existe un correo, documento o usuario, la operacion se considera completada sin
 * persistir y sin revelar el motivo.</p>
 */
@Service
public class ScoutRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ScoutRegistrationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public ScoutRegistrationService(UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    TransactionTemplate transactionTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Registra una solicitud de ojeador. Es idempotente ante duplicados: no crea
     * una segunda cuenta ni informa si el dato ya existia.
     */
    public void register(RegisterScoutRequest request) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());
        String documentNumber = normalizeDocumentNumber(request.documentNumber());
        String phone = normalizePhone(request.phone());

        if (userRepository.existsByEmail(email)
                || userRepository.existsByUsername(username)
                || userRepository.existsByDocumentNumber(documentNumber)) {
            log.debug("Solicitud de ojeador descartada por duplicado.");
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setDocumentType(request.documentType());
        user.setDocumentNumber(documentNumber);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.SCOUT);
        user.setStatus(UserStatus.PENDING_APPROVAL);
        user.setMustChangePassword(false);
        user.setTemporaryPasswordExpiresAt(null);

        try {
            transactionTemplate.executeWithoutResult(status -> userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            // Carrera con otro registro simultaneo: mismo resultado generico.
            log.debug("Solicitud de ojeador descartada por conflicto de datos.");
        }
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase();
    }

    private String normalizeUsername(String value) {
        return value.trim().toLowerCase();
    }

    private String normalizeDocumentNumber(String value) {
        return value.trim().toUpperCase();
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
