package com.scouting.scouting.player;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.dto.PlayerCreateRequest;
import com.scouting.scouting.player.entity.DominantFoot;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.repository.PlayerGuardianRepository;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.player.service.PlayerAccessService;
import com.scouting.scouting.player.service.PlayerMapper;
import com.scouting.scouting.player.service.PlayerProfileService;
import com.scouting.scouting.player.service.PlayerTime;
import com.scouting.scouting.security.AppUserPrincipal;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
import com.scouting.scouting.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerProfileServiceTest {

    @Test
    void createLocksTheTargetAccountBeforeResolvingRoleAndAcademy() {
        PlayerProfileRepository profileRepository = mock(PlayerProfileRepository.class);
        PlayerGuardianRepository guardianRepository = mock(PlayerGuardianRepository.class);
        PlayerHistoryEntryRepository historyRepository = mock(PlayerHistoryEntryRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserOrganizationRepository organizationRepository = mock(UserOrganizationRepository.class);
        PlayerAccessService accessService = mock(PlayerAccessService.class);
        PlayerMapper mapper = mock(PlayerMapper.class);
        UbigeoCatalog ubigeoCatalog = mock(UbigeoCatalog.class);

        PlayerProfileService service = new PlayerProfileService(
                profileRepository, guardianRepository, historyRepository, userRepository,
                organizationRepository, accessService, mapper, ubigeoCatalog);

        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        User target = new User();
        target.setId(2L);
        target.setRole(Role.PLAYER);
        target.setStatus(UserStatus.ACTIVE);
        target.setFirstName("Luis");
        target.setLastName("Quispe");

        Academy academy = new Academy();
        academy.setId(3L);
        academy.setName("Academia");
        academy.setCode("ACA-000003");
        academy.setOperationalStatus(OperationalStatus.ACTIVE);

        UserOrganization organization = new UserOrganization();
        organization.setUser(target);
        organization.setAcademy(academy);

        PlayerCreateRequest request = new PlayerCreateRequest(
                2L,
                PlayerTime.today().minusYears(16),
                "150101",
                170,
                new BigDecimal("65.00"),
                PlayerPosition.STRIKER,
                null,
                DominantFoot.RIGHT,
                null,
                null);

        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(target));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(organizationRepository.findByUser_Id(2L)).thenReturn(Optional.of(organization));
        when(profileRepository.saveAndFlush(any(PlayerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ubigeoCatalog.requireDistrict("150101")).thenReturn(
                new UbigeoCatalog.UbigeoDistrict("15", "Lima", "1501", "Lima", "150101", "Lima"));

        AppUserPrincipal actor = new AppUserPrincipal(
                1L, "admin@example.com", "hash", Role.ADMIN, UserStatus.ACTIVE, false, null);

        service.create(actor, request);

        verify(userRepository).findByIdForUpdate(2L);
        verify(userRepository, never()).findById(2L);
    }
}
