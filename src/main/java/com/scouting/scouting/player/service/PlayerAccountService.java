package com.scouting.scouting.player.service;

import com.scouting.scouting.player.dto.PlayerAccountCreateRequest;
import com.scouting.scouting.player.dto.PlayerAccountCreatedResponse;
import com.scouting.scouting.player.dto.PlayerCreateRequest;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.dto.UserCreatedResponse;
import com.scouting.scouting.user.service.UserManagementService;
import com.scouting.scouting.user.service.UserManagementService.PlayerAccountIdentityInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta conjunta de cuenta PLAYER y perfil deportivo (SCP-015).
 *
 * <p>Ambas escrituras comparten una unica transaccion: si la creacion del perfil
 * falla, se revierte tambien la cuenta y su asociacion de academia. La cuenta se
 * crea mediante el flujo interno de {@link UserManagementService}, que reutiliza
 * las mismas validaciones de identidad, duplicados y hashing de SCP-004.</p>
 */
@Service
public class PlayerAccountService {

    private final UserManagementService userManagementService;
    private final PlayerProfileService playerProfileService;

    public PlayerAccountService(UserManagementService userManagementService,
                                PlayerProfileService playerProfileService) {
        this.userManagementService = userManagementService;
        this.playerProfileService = playerProfileService;
    }

    @Transactional
    public PlayerAccountCreatedResponse create(AppUserPrincipal actor, PlayerAccountCreateRequest request) {
        UserCreatedResponse account = userManagementService.createPlayerAccount(
                actor,
                request.academyId(),
                new PlayerAccountIdentityInput(
                        request.firstName(),
                        request.lastName(),
                        request.documentType(),
                        request.documentNumber(),
                        request.phone(),
                        request.email(),
                        request.username()));

        Object player = playerProfileService.create(actor, new PlayerCreateRequest(
                account.user().id(),
                request.birthDate(),
                request.residenceUbigeoCode(),
                request.heightCm(),
                request.weightKg(),
                request.position(),
                request.bestPosition(),
                request.dominantFoot(),
                request.summary(),
                request.photoUrl()));

        return new PlayerAccountCreatedResponse(
                player, account.temporaryPassword(), account.temporaryPasswordExpiresAt());
    }
}
