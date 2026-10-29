package com.scouting.scouting.user.service;

import com.scouting.scouting.admin.dto.PageResponse;
import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.organization.service.OrganizationScopeService;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.dto.AcademyUserCreateRequest;
import com.scouting.scouting.user.dto.AdminUserCreateRequest;
import com.scouting.scouting.user.dto.UserAssignmentUpdateRequest;
import com.scouting.scouting.user.dto.UserCreatedResponse;
import com.scouting.scouting.user.dto.UserDetailResponse;
import com.scouting.scouting.user.dto.UserIdentityUpdateRequest;
import com.scouting.scouting.user.dto.UserStatusUpdateRequest;
import com.scouting.scouting.user.dto.UserSummaryResponse;
import com.scouting.scouting.user.entity.DocumentType;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import com.scouting.scouting.user.service.TemporaryPasswordGenerator.GeneratedTemporaryPassword;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gestion de cuentas y asignaciones (SCP-004).
 *
 * <p>La autorizacion se valida aqui ademas de en los controladores. El alcance de
 * una academia se obtiene siempre de la sesion; ningun endpoint de alcance propio
 * acepta academia, club, estado ni rol fuera de la matriz permitida. Las cuentas
 * internas nuevas se crean ACTIVE con una contrasena temporal de un solo uso y
 * jamas se exponen hashes ni secretos en los DTOs de lectura.</p>
 */
@Service
public class UserManagementService {

    private static final Set<Role> ADMIN_ONLY = Set.of(Role.ADMIN);
    private static final Set<Role> SCOPED_CREATE_ROLES = Set.of(Role.ACADEMY_DIRECTOR, Role.PROFESSOR);
    private static final Set<Role> PLAYER_ACCOUNT_CREATOR_ROLES =
            Set.of(Role.ADMIN, Role.ACADEMY_DIRECTOR, Role.PROFESSOR);
    private static final Set<Role> ACADEMY_ROLES = Set.of(Role.PROFESSOR, Role.ACADEMY_DIRECTOR, Role.PLAYER);
    private static final String GENERIC_DUPLICATE_MESSAGE = "Ya existe una cuenta con los datos indicados.";

    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final AcademyRepository academyRepository;
    private final ClubRepository clubRepository;
    private final OrganizationScopeService scopeService;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final PlayerProfileRepository playerProfileRepository;

    public UserManagementService(UserRepository userRepository,
                                 UserOrganizationRepository userOrganizationRepository,
                                 AcademyRepository academyRepository,
                                 ClubRepository clubRepository,
                                 OrganizationScopeService scopeService,
                                 PasswordEncoder passwordEncoder,
                                 TemporaryPasswordGenerator temporaryPasswordGenerator,
                                 PlayerProfileRepository playerProfileRepository) {
        this.userRepository = userRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.academyRepository = academyRepository;
        this.clubRepository = clubRepository;
        this.scopeService = scopeService;
        this.passwordEncoder = passwordEncoder;
        this.temporaryPasswordGenerator = temporaryPasswordGenerator;
        this.playerProfileRepository = playerProfileRepository;
    }

    // ---------------------------------------------------------------------
    // Lectura administrativa
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> listAdmin(AppUserPrincipal actor,
                                                       int page,
                                                       int size,
                                                       String sort,
                                                       String q,
                                                       Role role,
                                                       UserStatus status,
                                                       Long academyId,
                                                       Long clubId) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        if (academyId != null && clubId != null) {
            throw ApiException.badRequest("INVALID_FILTER_COMBINATION",
                    "No se puede filtrar por academia y club a la vez.");
        }
        if (status == UserStatus.PENDING_APPROVAL) {
            throw ApiException.badRequest("INVALID_STATUS_FILTER",
                    "Las solicitudes pendientes se consultan en la revision de ojeadores.");
        }

        List<UserStatus> statuses = status == null
                ? List.of(UserStatus.ACTIVE, UserStatus.INACTIVE)
                : List.of(status);
        Pageable pageable = UserPageRequestFactory.create(page, size, sort);
        Page<User> result = userRepository.searchAdmin(
                statuses, role, academyId, clubId, UserSearchQuery.containsPattern(q), pageable);
        Map<Long, UserOrganization> organizations = organizationsByUser(result.getContent());
        return PageResponse.from(result.map(
                user -> UserSummaryResponse.from(user, organizations.get(user.getId()))));
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getAdmin(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        return detail(findUser(id));
    }

    // ---------------------------------------------------------------------
    // Creacion
    // ---------------------------------------------------------------------

    @Transactional
    public UserCreatedResponse createAdmin(AppUserPrincipal actor, AdminUserCreateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        rejectPlayerOnGeneralCreation(request.role(), false);

        IdentityValues identity = identity(request.firstName(), request.lastName(), request.documentType(),
                request.documentNumber(), request.phone(), request.email(), request.username());
        validateIdentityAvailable(identity, null);

        OrganizationSelection selection = resolveSelection(
                request.role(), request.academyId(), request.clubId(), request.position());
        GeneratedTemporaryPassword temporaryPassword = temporaryPasswordGenerator.generate();
        User user = buildUser(identity, request.role(), temporaryPassword);
        User saved = saveNewUser(user);
        UserOrganization organization = createAssociation(saved, selection);

        return new UserCreatedResponse(
                UserSummaryResponse.from(saved, organization),
                temporaryPassword.password(),
                temporaryPassword.expiresAt());
    }

    @Transactional
    public UserCreatedResponse createScoped(AppUserPrincipal actor, AcademyUserCreateRequest request) {
        scopeService.requireRole(actor, SCOPED_CREATE_ROLES);
        Academy scoped = scopeService.requireAcademy(actor, SCOPED_CREATE_ROLES);

        rejectPlayerOnGeneralCreation(request.role(), true);
        if (!isScopedRoleAllowed(actor.role(), request.role())) {
            throw ApiException.forbidden("ROLE_NOT_ALLOWED",
                    "No puede crear cuentas con el rol solicitado.");
        }
        Academy academy = lockActiveAcademy(scoped.getId());

        IdentityValues identity = identity(request.firstName(), request.lastName(), request.documentType(),
                request.documentNumber(), request.phone(), request.email(), request.username());
        validateIdentityAvailable(identity, null);

        GeneratedTemporaryPassword temporaryPassword = temporaryPasswordGenerator.generate();
        User user = buildUser(identity, request.role(), temporaryPassword);
        User saved = saveNewUser(user);
        UserOrganization organization = createAssociation(
                saved, new OrganizationSelection(academy, null, null));

        return new UserCreatedResponse(
                UserSummaryResponse.from(saved, organization),
                temporaryPassword.password(),
                temporaryPassword.expiresAt());
    }

    /**
     * Alta de una cuenta PLAYER con su academia, de uso exclusivo del flujo
     * conjunto de Jugadores (SCP-015). Reutiliza la validacion de identidad,
     * deteccion de duplicados, hashing y contrasena temporal de este servicio.
     *
     * <p>Como el llamador es un servicio y no un controlador, aqui se vuelve a
     * validar el rol del actor. La academia del staff se deriva siempre de la
     * sesion; solo ADMIN puede indicarla. El metodo participa en la transaccion
     * del llamador para que un fallo posterior revierta tambien la cuenta.</p>
     */
    @Transactional
    public UserCreatedResponse createPlayerAccount(AppUserPrincipal actor,
                                                   Long requestedAcademyId,
                                                   PlayerAccountIdentityInput input) {
        scopeService.requireRole(actor, PLAYER_ACCOUNT_CREATOR_ROLES);
        Academy academy = resolvePlayerAcademy(actor, requestedAcademyId);

        IdentityValues identity = identity(input.firstName(), input.lastName(), input.documentType(),
                input.documentNumber(), input.phone(), input.email(), input.username());
        validateIdentityAvailable(identity, null);

        GeneratedTemporaryPassword temporaryPassword = temporaryPasswordGenerator.generate();
        User user = buildUser(identity, Role.PLAYER, temporaryPassword);
        User saved = saveNewUser(user);
        UserOrganization organization = createAssociation(
                saved, new OrganizationSelection(academy, null, null));

        return new UserCreatedResponse(
                UserSummaryResponse.from(saved, organization),
                temporaryPassword.password(),
                temporaryPassword.expiresAt());
    }

    // ---------------------------------------------------------------------
    // Edicion administrativa
    // ---------------------------------------------------------------------

    @Transactional
    public UserDetailResponse updateIdentity(AppUserPrincipal actor, Long id, UserIdentityUpdateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        User user = findForUpdate(id);
        rejectPendingMutation(user);

        IdentityValues identity = identity(request.firstName(), request.lastName(), request.documentType(),
                request.documentNumber(), request.phone(), request.email(), request.username());
        validateIdentityAvailable(identity, id);
        applyIdentity(user, identity);
        return detail(saveExistingUser(user));
    }

    @Transactional
    public UserDetailResponse updateAssignment(AppUserPrincipal actor, Long id,
                                               UserAssignmentUpdateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        // Bloqueo global de administradores activos antes de tocar la cuenta:
        // evita que dos degradaciones simultaneas dejen el sistema sin ADMIN.
        List<User> activeAdmins = lockActiveAdmins();
        User user = findForUpdate(id);
        rejectPendingMutation(user);

        Role newRole = request.role();
        if (actor.id().equals(user.getId()) && newRole != Role.ADMIN) {
            throw ApiException.conflict("SELF_ROLE_CHANGE_NOT_ALLOWED",
                    "Un administrador no puede degradar su propia cuenta.");
        }
        if (removesActiveAdmin(user, newRole, user.getStatus())) {
            requireAnotherActiveAdmin(activeAdmins, user.getId());
        }
        if (newRole != Role.PLAYER && playerProfileRepository.existsByUser_Id(user.getId())) {
            throw ApiException.conflict("PLAYER_PROFILE_EXISTS",
                    "La cuenta tiene un perfil deportivo; no se puede cambiar su rol.");
        }

        OrganizationSelection selection = resolveSelection(
                newRole, request.academyId(), request.clubId(), request.position());
        user.setRole(newRole);
        User saved = saveExistingUser(user);
        UserOrganization organization = applyAssociation(saved, selection);
        return UserDetailResponse.from(saved, organization);
    }

    @Transactional
    public UserDetailResponse updateStatus(AppUserPrincipal actor, Long id, UserStatusUpdateRequest request) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        List<User> activeAdmins = lockActiveAdmins();
        User user = findForUpdate(id);
        rejectPendingMutation(user);

        UserStatus target = request.status();
        if (target != UserStatus.ACTIVE && target != UserStatus.INACTIVE) {
            throw ApiException.badRequest("INVALID_STATUS",
                    "Solo se admite ACTIVE o INACTIVE en este endpoint.");
        }
        if (actor.id().equals(user.getId()) && target == UserStatus.INACTIVE) {
            throw ApiException.conflict("SELF_DEACTIVATION_NOT_ALLOWED",
                    "Un administrador no puede inhabilitar su propia cuenta.");
        }
        if (removesActiveAdmin(user, user.getRole(), target)) {
            requireAnotherActiveAdmin(activeAdmins, user.getId());
        }

        user.setStatus(target);
        User saved = saveExistingUser(user);
        return detail(saved);
    }

    @Transactional
    public UserCreatedResponse resetPassword(AppUserPrincipal actor, Long id) {
        scopeService.requireRole(actor, ADMIN_ONLY);
        User user = findForUpdate(id);
        if (user.getStatus() == UserStatus.PENDING_APPROVAL) {
            throw pendingNotManaged();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw ApiException.conflict("ACCOUNT_NOT_ACTIVE",
                    "Solo se puede restablecer la contrasena de una cuenta activa.");
        }

        GeneratedTemporaryPassword temporaryPassword = temporaryPasswordGenerator.generate();
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword.password()));
        user.setMustChangePassword(true);
        user.setTemporaryPasswordExpiresAt(temporaryPassword.expiresAt());
        User saved = saveExistingUser(user);
        UserOrganization organization = userOrganizationRepository.findByUser_Id(saved.getId()).orElse(null);

        return new UserCreatedResponse(
                UserSummaryResponse.from(saved, organization),
                temporaryPassword.password(),
                temporaryPassword.expiresAt());
    }

    // ---------------------------------------------------------------------
    // Identidad, duplicados y persistencia
    // ---------------------------------------------------------------------

    private User buildUser(IdentityValues identity, Role role, GeneratedTemporaryPassword temporaryPassword) {
        User user = new User();
        applyIdentity(user, identity);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword.password()));
        user.setMustChangePassword(true);
        user.setTemporaryPasswordExpiresAt(temporaryPassword.expiresAt());
        return user;
    }

    private void applyIdentity(User user, IdentityValues identity) {
        user.setFirstName(identity.firstName());
        user.setLastName(identity.lastName());
        user.setDocumentType(identity.documentType());
        user.setDocumentNumber(identity.documentNumber());
        user.setPhone(identity.phone());
        user.setEmail(identity.email());
        user.setUsername(identity.username());
    }

    private User saveNewUser(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateConflict();
        }
    }

    private User saveExistingUser(User user) {
        try {
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateConflict();
        }
    }

    private void validateIdentityAvailable(IdentityValues identity, Long excludeId) {
        boolean duplicate = excludeId == null
                ? userRepository.existsByEmail(identity.email())
                    || userRepository.existsByUsername(identity.username())
                    || userRepository.existsByDocumentNumber(identity.documentNumber())
                : userRepository.existsByEmailAndIdNot(identity.email(), excludeId)
                    || userRepository.existsByUsernameAndIdNot(identity.username(), excludeId)
                    || userRepository.existsByDocumentNumberAndIdNot(identity.documentNumber(), excludeId);
        if (duplicate) {
            throw duplicateConflict();
        }
    }

    private ApiException duplicateConflict() {
        return ApiException.conflict("USER_ALREADY_EXISTS", GENERIC_DUPLICATE_MESSAGE);
    }

    private IdentityValues identity(String firstName, String lastName, DocumentType documentType,
                                    String documentNumber, String phone, String email, String username) {
        return new IdentityValues(
                required(firstName, "Los nombres son obligatorios."),
                required(lastName, "Los apellidos son obligatorios."),
                documentType,
                required(documentNumber, "El numero de documento es obligatorio.").toUpperCase(Locale.ROOT),
                optional(phone),
                required(email, "El correo es obligatorio.").toLowerCase(Locale.ROOT),
                required(username, "El nombre de usuario es obligatorio.").toLowerCase(Locale.ROOT)
        );
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest("VALIDATION_ERROR", message);
        }
        return value.trim();
    }

    private String optional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePosition(String value) {
        return optional(value);
    }

    // ---------------------------------------------------------------------
    // Organizacion y matriz rol-organizacion
    // ---------------------------------------------------------------------

    private OrganizationSelection resolveSelection(Role role, Long academyId, Long clubId, String rawPosition) {
        if (role == null) {
            throw ApiException.badRequest("INVALID_ORGANIZATION_ASSIGNMENT", "El rol es obligatorio.");
        }
        String position = normalizePosition(rawPosition);

        if (ACADEMY_ROLES.contains(role)) {
            if (academyId == null || clubId != null || position != null) {
                throw invalidAssignment();
            }
            return new OrganizationSelection(lockActiveAcademy(academyId), null, null);
        }
        if (role == Role.CLUB_MANAGER) {
            if (clubId == null || academyId != null) {
                throw invalidAssignment();
            }
            return new OrganizationSelection(null, lockActiveClub(clubId), position);
        }
        if (academyId != null || clubId != null || position != null) {
            throw invalidAssignment();
        }
        return new OrganizationSelection(null, null, null);
    }

    private Academy lockActiveAcademy(Long id) {
        Academy academy = academyRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("ORGANIZATION_NOT_FOUND", "Organizacion no encontrada."));
        if (academy.getOperationalStatus() != OperationalStatus.ACTIVE) {
            throw ApiException.conflict("ORGANIZATION_INACTIVE",
                    "La academia esta inhabilitada y no admite nuevas cuentas ni asignaciones.");
        }
        return academy;
    }

    private Club lockActiveClub(Long id) {
        Club club = clubRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("ORGANIZATION_NOT_FOUND", "Organizacion no encontrada."));
        if (club.getOperationalStatus() != OperationalStatus.ACTIVE) {
            throw ApiException.conflict("ORGANIZATION_INACTIVE",
                    "El club esta inhabilitado y no admite nuevas cuentas ni asignaciones.");
        }
        return club;
    }

    private ApiException invalidAssignment() {
        return ApiException.badRequest("INVALID_ORGANIZATION_ASSIGNMENT",
                "La organizacion enviada no corresponde al rol.");
    }

    private boolean isScopedRoleAllowed(Role actorRole, Role requestedRole) {
        if (requestedRole == Role.PLAYER) {
            return false;
        }
        if (actorRole == Role.PROFESSOR) {
            return false;
        }
        if (actorRole == Role.ACADEMY_DIRECTOR) {
            return requestedRole == Role.PROFESSOR;
        }
        return false;
    }

    /**
     * PLAYER deja de crearse desde Usuarios: el alta conjunta con el perfil
     * deportivo se realiza en el modulo Jugadores. El mensaje explica la ruta
     * correcta sin revelar datos de cuentas existentes.
     */
    private void rejectPlayerOnGeneralCreation(Role requestedRole, boolean scoped) {
        if (requestedRole != Role.PLAYER) {
            return;
        }
        String message = "Las cuentas PLAYER se crean junto con su perfil deportivo "
                + "desde el modulo Jugadores.";
        if (scoped) {
            throw ApiException.forbidden("ROLE_NOT_ALLOWED", message);
        }
        throw ApiException.badRequest("INVALID_ROLE", message);
    }

    private Academy resolvePlayerAcademy(AppUserPrincipal actor, Long requestedAcademyId) {
        if (actor.role() == Role.ADMIN) {
            if (requestedAcademyId == null) {
                throw ApiException.badRequest("ACADEMY_REQUIRED",
                        "La academia es obligatoria para crear un jugador.");
            }
            return lockActiveAcademy(requestedAcademyId);
        }
        Academy scoped = scopeService.requireAcademy(actor, SCOPED_CREATE_ROLES);
        return lockActiveAcademy(scoped.getId());
    }

    private UserOrganization createAssociation(User user, OrganizationSelection selection) {
        if (!selection.hasOrganization()) {
            return null;
        }
        UserOrganization association = new UserOrganization();
        association.setUser(user);
        association.setAcademy(selection.academy());
        association.setClub(selection.club());
        association.setPosition(selection.position());
        return userOrganizationRepository.saveAndFlush(association);
    }

    private UserOrganization applyAssociation(User user, OrganizationSelection selection) {
        UserOrganization association = userOrganizationRepository.findByUser_Id(user.getId()).orElse(null);
        if (!selection.hasOrganization()) {
            if (association != null) {
                userOrganizationRepository.delete(association);
                userOrganizationRepository.flush();
            }
            return null;
        }
        if (association == null) {
            association = new UserOrganization();
            association.setUser(user);
        }
        association.setAcademy(selection.academy());
        association.setClub(selection.club());
        association.setPosition(selection.position());
        return userOrganizationRepository.saveAndFlush(association);
    }

    // ---------------------------------------------------------------------
    // Protecciones de administrador y lectura de detalle
    // ---------------------------------------------------------------------

    private List<User> lockActiveAdmins() {
        return userRepository.findByRoleAndStatusForUpdate(Role.ADMIN, UserStatus.ACTIVE);
    }

    private boolean removesActiveAdmin(User user, Role targetRole, UserStatus targetStatus) {
        return user.getRole() == Role.ADMIN
                && user.getStatus() == UserStatus.ACTIVE
                && (targetRole != Role.ADMIN || targetStatus != UserStatus.ACTIVE);
    }

    private void requireAnotherActiveAdmin(List<User> activeAdmins, Long targetId) {
        boolean hasAnother = activeAdmins.stream()
                .anyMatch(admin -> !admin.getId().equals(targetId));
        if (!hasAnother) {
            throw ApiException.conflict("LAST_ADMIN_PROTECTED",
                    "No es posible dejar el sistema sin un administrador activo.");
        }
    }

    private void rejectPendingMutation(User user) {
        if (user.getStatus() == UserStatus.PENDING_APPROVAL) {
            throw pendingNotManaged();
        }
    }

    private ApiException pendingNotManaged() {
        return ApiException.conflict("PENDING_ACCOUNT_NOT_MANAGED",
                "Las cuentas pendientes se gestionan desde la revision de solicitudes de ojeador.");
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Cuenta no encontrada."));
    }

    private User findForUpdate(Long id) {
        return userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Cuenta no encontrada."));
    }

    private UserDetailResponse detail(User user) {
        UserOrganization organization = userOrganizationRepository.findByUser_Id(user.getId()).orElse(null);
        return UserDetailResponse.from(user, organization);
    }

    private Map<Long, UserOrganization> organizationsByUser(Collection<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = users.stream().map(User::getId).toList();
        return userOrganizationRepository.findByUser_IdIn(ids).stream()
                .collect(Collectors.toMap(
                        association -> association.getUser().getId(),
                        Function.identity(),
                        (first, second) -> first));
    }

    // ---------------------------------------------------------------------
    // Tipos internos
    // ---------------------------------------------------------------------

    private record IdentityValues(
            String firstName,
            String lastName,
            DocumentType documentType,
            String documentNumber,
            String phone,
            String email,
            String username
    ) {
    }

    private record OrganizationSelection(Academy academy, Club club, String position) {

        boolean hasOrganization() {
            return academy != null || club != null;
        }
    }

    /**
     * Identidad minima para el alta interna de una cuenta PLAYER. No incluye rol,
     * estado, organizacion ni contrasena: esos valores los fija el servidor.
     */
    public record PlayerAccountIdentityInput(
            String firstName,
            String lastName,
            DocumentType documentType,
            String documentNumber,
            String phone,
            String email,
            String username
    ) {
    }
}
