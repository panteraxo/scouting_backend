package com.scouting.scouting.player.service;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.organization.service.OrganizationScopeService;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.UserStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Resuelve visibilidad y alcance de perfiles. La academia se obtiene siempre
 * de UserOrganization y nunca de un identificador enviado por el cliente.
 */
@Service
public class PlayerAccessService {

    private static final Set<Role> VIEW_ROLES = Set.of(
            Role.ADMIN, Role.ACADEMY_DIRECTOR, Role.PROFESSOR, Role.SCOUT, Role.PLAYER);
    private static final Set<Role> MANAGE_ROLES = Set.of(
            Role.ADMIN, Role.ACADEMY_DIRECTOR, Role.PROFESSOR);
    private static final Set<Role> STAFF_ROLES = Set.of(Role.ACADEMY_DIRECTOR, Role.PROFESSOR);

    private final PlayerProfileRepository playerProfileRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationScopeService scopeService;

    public PlayerAccessService(PlayerProfileRepository playerProfileRepository,
                               UserOrganizationRepository userOrganizationRepository,
                               OrganizationScopeService scopeService) {
        this.playerProfileRepository = playerProfileRepository;
        this.userOrganizationRepository = userOrganizationRepository;
        this.scopeService = scopeService;
    }

    @Transactional(readOnly = true)
    public PlayerProfile loadProfile(Long id) {
        return playerProfileRepository.findDetailById(id).orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public UserOrganization organization(PlayerProfile profile) {
        return userOrganizationRepository.findByUser_Id(profile.getUser().getId())
                .orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public Academy requireActorAcademy(AppUserPrincipal actor) {
        return scopeService.requireAcademy(actor, STAFF_ROLES);
    }

    @Transactional(readOnly = true)
    public PlayerProfile requireView(AppUserPrincipal actor, Long id) {
        requireRole(actor, VIEW_ROLES);
        PlayerProfile profile = loadProfile(id);
        switch (actor.role()) {
            case ADMIN -> {
                return profile;
            }
            case ACADEMY_DIRECTOR, PROFESSOR -> {
                UserOrganization organization = organization(profile);
                Academy academy = requireActorAcademy(actor);
                if (organization.getAcademy() == null
                        || !academy.getId().equals(organization.getAcademy().getId())) {
                    throw notFound();
                }
                return profile;
            }
            case SCOUT -> {
                if (!isScoutEligible(profile, organization(profile))) {
                    throw notFound();
                }
                return profile;
            }
            case PLAYER -> {
                if (!profile.getUser().getId().equals(actor.id())) {
                    throw notFound();
                }
                return profile;
            }
            default -> throw forbidden();
        }
    }

    @Transactional(readOnly = true)
    public PlayerProfile requireManage(AppUserPrincipal actor, Long id) {
        requireRole(actor, MANAGE_ROLES);
        PlayerProfile profile = loadProfile(id);
        requireStaffScope(actor, profile);
        return profile;
    }

    @Transactional(readOnly = true)
    public PlayerProfile requireStaff(AppUserPrincipal actor, Long id) {
        requireStaffRole(actor);
        PlayerProfile profile = loadProfile(id);
        requireStaffScope(actor, profile);
        return profile;
    }

    /**
     * Valida el alcance de un perfil ya bloqueado. Se usa cuando el llamador
     * necesita adquirir el bloqueo antes de leer datos sensibles, para no crear
     * una instantanea de lectura previa al bloqueo.
     */
    @Transactional(readOnly = true)
    public PlayerProfile requireStaff(AppUserPrincipal actor, PlayerProfile profile) {
        requireStaffRole(actor);
        requireStaffScope(actor, profile);
        return profile;
    }

    public void requireStaffRole(AppUserPrincipal actor) {
        if (actor == null || !STAFF_ROLES.contains(actor.role())) {
            throw ApiException.forbidden("GUARDIAN_ACCESS_DENIED",
                    "No tiene permisos para acceder a los datos del tutor.");
        }
    }

    @Transactional(readOnly = true)
    public PlayerProfile requireScoutEligible(AppUserPrincipal actor, Long id) {
        requireRole(actor, Set.of(Role.SCOUT));
        PlayerProfile profile = loadProfile(id);
        if (!isScoutEligible(profile, organization(profile))) {
            throw notFound();
        }
        return profile;
    }

    @Transactional(readOnly = true)
    public PlayerProfile requireOwn(AppUserPrincipal actor) {
        requireRole(actor, Set.of(Role.PLAYER));
        return playerProfileRepository.findByUser_Id(actor.id()).orElseThrow(this::notFound);
    }

    @Transactional(readOnly = true)
    public void requireWritableByStaff(AppUserPrincipal actor, PlayerProfile profile) {
        if (actor.role() == Role.ADMIN) {
            return;
        }
        if (profile.getOperationalStatus() == PlayerOperationalStatus.INACTIVE) {
            throw ApiException.conflict("PLAYER_PROFILE_INACTIVE",
                    "El perfil esta inhabilitado; solo un administrador puede reactivarlo.");
        }
        UserOrganization organization = organization(profile);
        if (organization.getAcademy() == null
                || organization.getAcademy().getOperationalStatus() != OperationalStatus.ACTIVE) {
            throw ApiException.conflict("ACADEMY_INACTIVE",
                    "La academia esta inhabilitada y no admite cambios en jugadores.");
        }
    }

    public void requireAcademyActiveForCreate(UserOrganization organization) {
        if (organization.getAcademy() == null
                || organization.getAcademy().getOperationalStatus() != OperationalStatus.ACTIVE) {
            throw ApiException.conflict("ACADEMY_INACTIVE",
                    "La academia esta inhabilitada y no admite nuevos perfiles.");
        }
    }

    public boolean isScoutEligible(PlayerProfile profile, UserOrganization organization) {
        return profile.getOperationalStatus() == PlayerOperationalStatus.ACTIVE
                && profile.getProfileStatus() == PlayerProfileStatus.COMPLETE
                && profile.getUser().getStatus() == UserStatus.ACTIVE
                && organization != null
                && organization.getAcademy() != null
                && organization.getAcademy().getOperationalStatus() == OperationalStatus.ACTIVE;
    }

    public void requireRole(AppUserPrincipal actor, Set<Role> roles) {
        if (actor == null) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas.");
        }
        if (!roles.contains(actor.role())) {
            throw forbidden();
        }
    }

    private void requireStaffScope(AppUserPrincipal actor, PlayerProfile profile) {
        if (actor.role() == Role.ADMIN) {
            return;
        }
        UserOrganization organization = organization(profile);
        Academy academy = requireActorAcademy(actor);
        if (organization.getAcademy() == null
                || !academy.getId().equals(organization.getAcademy().getId())) {
            throw notFound();
        }
    }

    private ApiException notFound() {
        return ApiException.notFound("PLAYER_NOT_FOUND", "Jugador no encontrado.");
    }

    private ApiException forbidden() {
        return ApiException.forbidden("ACCESS_DENIED", "No tiene permisos para realizar esta accion.");
    }
}
