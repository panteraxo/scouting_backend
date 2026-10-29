package com.scouting.scouting.organization.service;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Resuelve la academia o club de la cuenta autenticada desde
 * {@code UserOrganization}. El identificador de la organizacion nunca se toma
 * del request en las rutas de alcance propio.
 */
@Service
public class OrganizationScopeService {

    private final UserOrganizationRepository userOrganizationRepository;

    public OrganizationScopeService(UserOrganizationRepository userOrganizationRepository) {
        this.userOrganizationRepository = userOrganizationRepository;
    }

    @Transactional(readOnly = true)
    public Academy requireAcademy(AppUserPrincipal actor, Set<Role> allowedRoles) {
        requireRole(actor, allowedRoles);
        UserOrganization association = userOrganizationRepository.findByUser_Id(actor.id())
                .orElseThrow(this::organizationNotFound);
        if (association.getAcademy() == null) {
            throw organizationNotFound();
        }
        return association.getAcademy();
    }

    @Transactional(readOnly = true)
    public Club requireClub(AppUserPrincipal actor, Set<Role> allowedRoles) {
        requireRole(actor, allowedRoles);
        UserOrganization association = userOrganizationRepository.findByUser_Id(actor.id())
                .orElseThrow(this::organizationNotFound);
        if (association.getClub() == null) {
            throw organizationNotFound();
        }
        return association.getClub();
    }

    public void requireRole(AppUserPrincipal actor, Set<Role> allowedRoles) {
        if (actor == null) {
            throw ApiException.unauthorized("AUTHENTICATION_REQUIRED", "Credenciales invalidas.");
        }
        if (!allowedRoles.contains(actor.role())) {
            throw ApiException.forbidden("ACCESS_DENIED", "No tiene permisos para realizar esta accion.");
        }
    }

    public void requireRole(AppUserPrincipal actor, Role... allowedRoles) {
        requireRole(actor, Set.of(allowedRoles));
    }

    private ApiException organizationNotFound() {
        return ApiException.notFound("ORGANIZATION_NOT_FOUND", "Organizacion no encontrada.");
    }
}
