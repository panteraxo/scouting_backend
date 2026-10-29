package com.scouting.scouting.user;

import com.scouting.scouting.exception.ApiException;
import com.scouting.scouting.organization.repository.AcademyRepository;
import com.scouting.scouting.organization.repository.ClubRepository;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.organization.service.OrganizationScopeService;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.user.dto.UserAssignmentUpdateRequest;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import com.scouting.scouting.user.service.TemporaryPasswordGenerator;
import com.scouting.scouting.user.service.UserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprueba la red de seguridad del ultimo ADMIN incluso en una llamada que no
 * pasa por la sesion HTTP (por ejemplo una operacion concurrente).
 */
class UserManagementServiceTest {

    @Test
    void lastActiveAdminCannotBeRemovedByAnotherAdministratorPrincipal() {
        UserRepository userRepository = mock(UserRepository.class);
        UserOrganizationRepository userOrganizationRepository = mock(UserOrganizationRepository.class);
        AcademyRepository academyRepository = mock(AcademyRepository.class);
        ClubRepository clubRepository = mock(ClubRepository.class);
        OrganizationScopeService scopeService = mock(OrganizationScopeService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        PlayerProfileRepository playerProfileRepository = mock(PlayerProfileRepository.class);

        UserManagementService service = new UserManagementService(
                userRepository, userOrganizationRepository, academyRepository, clubRepository,
                scopeService, passwordEncoder, new TemporaryPasswordGenerator(), playerProfileRepository);

        User onlyActiveAdmin = new User();
        onlyActiveAdmin.setId(7L);
        onlyActiveAdmin.setRole(Role.ADMIN);
        onlyActiveAdmin.setStatus(UserStatus.ACTIVE);

        when(userRepository.findByRoleAndStatusForUpdate(Role.ADMIN, UserStatus.ACTIVE))
                .thenReturn(List.of(onlyActiveAdmin));
        when(userRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(onlyActiveAdmin));

        AppUserPrincipal anotherAdmin = new AppUserPrincipal(
                99L, "other-admin@example.com", "hash", Role.ADMIN, UserStatus.ACTIVE, false, null);
        UserAssignmentUpdateRequest request = new UserAssignmentUpdateRequest(Role.SCOUT, null, null, null);

        ApiException exception = assertThrows(ApiException.class,
                () -> service.updateAssignment(anotherAdmin, 7L, request));

        assertEquals("LAST_ADMIN_PROTECTED", exception.getCode());
    }
}
