package com.scouting.scouting.player.service;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.organization.repository.UserOrganizationRepository;
import com.scouting.scouting.player.dto.ActorSummaryResponse;
import com.scouting.scouting.player.dto.AcademyRefResponse;
import com.scouting.scouting.player.dto.GuardianConsentResponse;
import com.scouting.scouting.player.dto.GuardianConsentSummaryResponse;
import com.scouting.scouting.player.dto.PlayerAdminDetailResponse;
import com.scouting.scouting.player.dto.PlayerDirectoryItemResponse;
import com.scouting.scouting.player.dto.PlayerGuardianScoutResponse;
import com.scouting.scouting.player.dto.PlayerGuardianStaffResponse;
import com.scouting.scouting.player.dto.PlayerHistoryEntryResponse;
import com.scouting.scouting.player.dto.PlayerHistoryMetricsDto;
import com.scouting.scouting.player.dto.PlayerHistoryRatingsDto;
import com.scouting.scouting.player.dto.PlayerOwnResponse;
import com.scouting.scouting.player.dto.PlayerScoutDetailResponse;
import com.scouting.scouting.player.dto.PlayerStaffDetailResponse;
import com.scouting.scouting.player.dto.ResidenceResponse;
import com.scouting.scouting.player.entity.GuardianConsentStatus;
import com.scouting.scouting.player.entity.PlayerGuardian;
import com.scouting.scouting.player.entity.PlayerGuardianConsentEvent;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerHistoryMetric;
import com.scouting.scouting.player.entity.PlayerMetricCode;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PositionGroup;
import com.scouting.scouting.player.repository.PlayerGuardianConsentEventRepository;
import com.scouting.scouting.player.repository.PlayerGuardianRepository;
import com.scouting.scouting.player.repository.PlayerHistoryEntryRepository;
import com.scouting.scouting.player.repository.PlayerProfileRepository;
import com.scouting.scouting.ubigeo.service.UbigeoCatalog;
import com.scouting.scouting.user.entity.User;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Period;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Conversion explicita de entidades SCP-005 a DTOs por rol. */
@Component
public class PlayerMapper {

    private final UbigeoCatalog ubigeoCatalog;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PlayerHistoryEntryRepository historyRepository;
    private final PlayerGuardianRepository guardianRepository;
    private final PlayerGuardianConsentEventRepository consentRepository;

    public PlayerMapper(UbigeoCatalog ubigeoCatalog,
                        UserOrganizationRepository userOrganizationRepository,
                        PlayerHistoryEntryRepository historyRepository,
                        PlayerGuardianRepository guardianRepository,
                        PlayerGuardianConsentEventRepository consentRepository) {
        this.ubigeoCatalog = ubigeoCatalog;
        this.userOrganizationRepository = userOrganizationRepository;
        this.historyRepository = historyRepository;
        this.guardianRepository = guardianRepository;
        this.consentRepository = consentRepository;
    }

    public ResidenceResponse residence(String ubigeoCode, boolean includeDistrict) {
        if (ubigeoCode == null) {
            return null;
        }
        return ubigeoCatalog.findDistrict(ubigeoCode)
                .map(district -> new ResidenceResponse(
                        district.departmentCode(),
                        district.departmentName(),
                        district.provinceCode(),
                        district.provinceName(),
                        includeDistrict ? district.districtCode() : null,
                        includeDistrict ? district.districtName() : null))
                .orElse(null);
    }

    public AcademyRefResponse academy(UserOrganization organization) {
        if (organization == null || organization.getAcademy() == null) {
            return null;
        }
        Academy academy = organization.getAcademy();
        return new AcademyRefResponse(
                academy.getId(),
                academy.getCode(),
                academy.getName(),
                academy.getLogoUrl(),
                academy.getOperationalStatus(),
                academy.getVerificationStatus());
    }

    public ActorSummaryResponse actor(User user) {
        if (user == null) {
            return null;
        }
        return new ActorSummaryResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getRole());
    }

    public Integer yearsExperience(PlayerProfile profile) {
        return yearsExperience(profile.getId(), historyRepository.findFirstActivityDate(profile.getId()));
    }

    public Map<Long, Integer> yearsExperienceByProfileIds(Collection<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (PlayerHistoryEntryRepository.PlayerFirstActivityProjection projection
                : historyRepository.findFirstActivityDates(profileIds)) {
            result.put(projection.getPlayerId(), yearsExperience(projection.getPlayerId(),
                    projection.getFirstActivityDate()));
        }
        return result;
    }

    private Integer yearsExperience(Long profileId, java.time.LocalDate firstActivityDate) {
        if (firstActivityDate == null) {
            return null;
        }
        int years = Period.between(firstActivityDate, PlayerTime.today()).getYears();
        return Math.max(years, 0);
    }

    public PlayerDirectoryItemResponse toDirectory(PlayerProfile profile,
                                                   UserOrganization organization,
                                                   Integer yearsExperience) {
        return directory(profile, organization, yearsExperience, true,
                profile.getOperationalStatus(), profile.getProfileStatus());
    }

    public PlayerDirectoryItemResponse toScoutDirectory(PlayerProfile profile,
                                                        UserOrganization organization,
                                                        Integer yearsExperience) {
        return directory(profile, organization, yearsExperience, false, null, null);
    }

    private PlayerDirectoryItemResponse directory(PlayerProfile profile,
                                                  UserOrganization organization,
                                                  Integer yearsExperience,
                                                  boolean includeDistrict,
                                                  com.scouting.scouting.player.entity.PlayerOperationalStatus operationalStatus,
                                                  com.scouting.scouting.player.entity.PlayerProfileStatus profileStatus) {
        return new PlayerDirectoryItemResponse(
                profile.getId(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                PlayerTime.age(profile.getBirthDate(), PlayerTime.today()),
                yearsExperience,
                profile.getPhotoUrl(),
                profile.getPosition(),
                profile.getPosition() == null ? null : profile.getPosition().group(),
                profile.getBestPosition(),
                profile.getBestPosition() == null ? null : profile.getBestPosition().group(),
                profile.getDominantFoot(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                residence(profile.getResidenceUbigeoCode(), includeDistrict),
                academy(organization),
                operationalStatus,
                profileStatus);
    }

    public PlayerOwnResponse toOwn(PlayerProfile profile, UserOrganization organization) {
        return new PlayerOwnResponse(
                profile.getId(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                profile.getBirthDate(),
                PlayerTime.age(profile.getBirthDate(), PlayerTime.today()),
                yearsExperience(profile),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getPosition(),
                profile.getPosition() == null ? null : profile.getPosition().group(),
                profile.getBestPosition(),
                profile.getBestPosition() == null ? null : profile.getBestPosition().group(),
                profile.getDominantFoot(),
                profile.getSummary(),
                profile.getPhotoUrl(),
                residence(profile.getResidenceUbigeoCode(), true),
                academy(organization),
                profile.getOperationalStatus(),
                profile.getProfileStatus(),
                false,
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getVersion());
    }

    public PlayerAdminDetailResponse toAdmin(PlayerProfile profile, UserOrganization organization) {
        boolean guardianRequired = PlayerTime.isMinor(profile.getBirthDate(), PlayerTime.today());
        return new PlayerAdminDetailResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                profile.getBirthDate(),
                PlayerTime.age(profile.getBirthDate(), PlayerTime.today()),
                yearsExperience(profile),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getPosition(),
                profile.getPosition() == null ? null : profile.getPosition().group(),
                profile.getBestPosition(),
                profile.getBestPosition() == null ? null : profile.getBestPosition().group(),
                profile.getDominantFoot(),
                profile.getSummary(),
                profile.getPhotoUrl(),
                residence(profile.getResidenceUbigeoCode(), true),
                academy(organization),
                profile.getOperationalStatus(),
                profile.getProfileStatus(),
                guardianRequired,
                actor(profile.getCreatedBy()),
                actor(profile.getUpdatedBy()),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getVersion());
    }

    public PlayerStaffDetailResponse toStaff(PlayerProfile profile, UserOrganization organization) {
        boolean minor = PlayerTime.isMinor(profile.getBirthDate(), PlayerTime.today());
        boolean guardianExists = guardianRepository.existsByPlayerProfile_Id(profile.getId());
        PlayerStaffDetailResponse.GuardianDataStatus dataStatus = !minor
                ? PlayerStaffDetailResponse.GuardianDataStatus.NOT_APPLICABLE
                : guardianExists
                        ? PlayerStaffDetailResponse.GuardianDataStatus.RECORDED
                        : PlayerStaffDetailResponse.GuardianDataStatus.MISSING;
        return new PlayerStaffDetailResponse(
                profile.getId(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                profile.getBirthDate(),
                PlayerTime.age(profile.getBirthDate(), PlayerTime.today()),
                yearsExperience(profile),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getPosition(),
                profile.getPosition() == null ? null : profile.getPosition().group(),
                profile.getBestPosition(),
                profile.getBestPosition() == null ? null : profile.getBestPosition().group(),
                profile.getDominantFoot(),
                profile.getSummary(),
                profile.getPhotoUrl(),
                residence(profile.getResidenceUbigeoCode(), true),
                academy(organization),
                profile.getOperationalStatus(),
                profile.getProfileStatus(),
                minor,
                dataStatus,
                minor ? consentStatus(profile.getId()) : GuardianConsentStatus.NOT_RECORDED,
                actor(profile.getCreatedBy()),
                actor(profile.getUpdatedBy()),
                profile.getCreatedAt(),
                profile.getUpdatedAt(),
                profile.getVersion());
    }

    public PlayerScoutDetailResponse toScout(PlayerProfile profile, UserOrganization organization) {
        boolean minor = PlayerTime.isMinor(profile.getBirthDate(), PlayerTime.today());
        boolean guardianDataAvailable = minor && guardianRepository.existsByPlayerProfile_Id(profile.getId());
        return new PlayerScoutDetailResponse(
                profile.getId(),
                profile.getUser().getFirstName(),
                profile.getUser().getLastName(),
                PlayerTime.age(profile.getBirthDate(), PlayerTime.today()),
                yearsExperience(profile),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getPosition(),
                profile.getPosition() == null ? null : profile.getPosition().group(),
                profile.getBestPosition(),
                profile.getBestPosition() == null ? null : profile.getBestPosition().group(),
                profile.getDominantFoot(),
                profile.getSummary(),
                profile.getPhotoUrl(),
                residence(profile.getResidenceUbigeoCode(), false),
                academy(organization),
                guardianDataAvailable);
    }

    public PlayerHistoryEntryResponse toHistory(PlayerHistoryEntry entry) {
        return new PlayerHistoryEntryResponse(
                entry.getId(),
                entry.getPlayerProfile().getId(),
                entry.getActivityType(),
                entry.getActivityDate(),
                entry.getCompetition(),
                entry.getOpponent(),
                entry.getAgeCategory(),
                entry.getCompetitiveLevel(),
                entry.getPositionPlayed(),
                entry.getPositionPlayed() == null ? null : entry.getPositionPlayed().group(),
                entry.getParticipation(),
                entry.getMinutesPlayed(),
                entry.getTeamResult(),
                entry.getTeamGoals(),
                entry.getOpponentGoals(),
                entry.getPlayerGoals(),
                entry.getAssists(),
                entry.getYellowCards(),
                entry.getRedCards(),
                entry.getObservations(),
                entry.getVideoUrl(),
                entry.getVideoTimestampSeconds(),
                entry.getVideoDescription(),
                metrics(entry),
                new PlayerHistoryRatingsDto(
                        entry.getTechnicalRating(),
                        entry.getTacticalRating(),
                        entry.getPhysicalRating(),
                        entry.getMentalRating(),
                        entry.getOverallRating()),
                entry.getStrengths(),
                entry.getImprovements(),
                entry.getRecommendations(),
                actor(entry.getCreatedBy()),
                actor(entry.getLastModifiedBy()),
                entry.getCreatedAt(),
                entry.getUpdatedAt(),
                entry.getVersion(),
                entry.getRevisionNumber());
    }

    public PlayerGuardianStaffResponse toGuardianStaff(PlayerGuardian guardian,
                                                       PlayerGuardianConsentEvent consent) {
        return new PlayerGuardianStaffResponse(
                guardian.getPlayerProfile().getId(),
                PlayerTime.isMinor(guardian.getPlayerProfile().getBirthDate(), PlayerTime.today()),
                guardian.getFirstName(),
                guardian.getLastName(),
                guardian.getDocumentType(),
                guardian.getDocumentNumber(),
                guardian.getPhone(),
                guardian.getAddress(),
                guardian.getRelationship(),
                consentResponse(consent),
                guardian.getVersion(),
                guardian.getUpdatedAt());
    }

    public PlayerGuardianScoutResponse toGuardianScout(PlayerGuardian guardian,
                                                       PlayerGuardianConsentEvent consent) {
        return new PlayerGuardianScoutResponse(
                guardian.getPlayerProfile().getId(),
                PlayerTime.isMinor(guardian.getPlayerProfile().getBirthDate(), PlayerTime.today()),
                guardian.getFirstName(),
                guardian.getLastName(),
                guardian.getDocumentType(),
                guardian.getDocumentNumber(),
                guardian.getPhone(),
                guardian.getAddress(),
                guardian.getRelationship(),
                new GuardianConsentSummaryResponse(
                        consent == null ? GuardianConsentStatus.NOT_RECORDED : consent.getStatus(),
                        consent == null ? null : consent.getRecordedAt()));
    }

    public GuardianConsentResponse consentResponse(PlayerGuardianConsentEvent consent) {
        if (consent == null) {
            return new GuardianConsentResponse(
                    GuardianConsentStatus.NOT_RECORDED, null, null, null, null);
        }
        return new GuardianConsentResponse(
                consent.getStatus(),
                consent.getRecordedAt(),
                actor(consent.getRecordedBy()),
                consent.getEvidenceUrl(),
                consent.getNote());
    }

    public GuardianConsentStatus consentStatus(Long profileId) {
        return consentRepository.findTopByPlayerProfile_IdOrderByRecordedAtDescIdDesc(profileId)
                .map(PlayerGuardianConsentEvent::getStatus)
                .orElse(GuardianConsentStatus.NOT_RECORDED);
    }

    public Optional<PlayerGuardian> guardian(Long profileId) {
        return guardianRepository.findByPlayerProfile_Id(profileId);
    }

    private PlayerHistoryMetricsDto metrics(PlayerHistoryEntry entry) {
        Map<String, BigDecimal> values = new HashMap<>();
        for (PlayerHistoryMetric metric : entry.getMetrics()) {
            values.put(metric.getMetricCode(), metric.getNumericValue());
        }

        PlayerHistoryMetricsDto.Goalkeeper goalkeeper = null;
        if (hasGroup(values, PositionGroup.GOALKEEPER)) {
            goalkeeper = new PlayerHistoryMetricsDto.Goalkeeper(
                    intValue(values, PlayerMetricCode.SAVES),
                    intValue(values, PlayerMetricCode.GOALS_CONCEDED),
                    booleanValue(values, PlayerMetricCode.CLEAN_SHEET),
                    intValue(values, PlayerMetricCode.CROSS_INTERVENTIONS),
                    intValue(values, PlayerMetricCode.GK_PASSES_ATTEMPTED),
                    intValue(values, PlayerMetricCode.GK_PASSES_COMPLETED));
        }

        PlayerHistoryMetricsDto.Defender defender = null;
        if (hasGroup(values, PositionGroup.DEFENDER)) {
            defender = new PlayerHistoryMetricsDto.Defender(
                    intValue(values, PlayerMetricCode.SUCCESSFUL_TACKLES),
                    intValue(values, PlayerMetricCode.DEF_INTERCEPTIONS),
                    intValue(values, PlayerMetricCode.CLEARANCES),
                    intValue(values, PlayerMetricCode.DEF_DUELS_WON),
                    intValue(values, PlayerMetricCode.BLOCKS),
                    intValue(values, PlayerMetricCode.ERRORS_LEADING_TO_CHANCE));
        }

        PlayerHistoryMetricsDto.Midfielder midfielder = null;
        if (hasGroup(values, PositionGroup.MIDFIELDER)) {
            midfielder = new PlayerHistoryMetricsDto.Midfielder(
                    intValue(values, PlayerMetricCode.MID_PASSES_ATTEMPTED),
                    intValue(values, PlayerMetricCode.MID_PASSES_COMPLETED),
                    intValue(values, PlayerMetricCode.MID_INTERCEPTIONS),
                    intValue(values, PlayerMetricCode.KEY_PASSES),
                    intValue(values, PlayerMetricCode.MID_ASSISTS),
                    intValue(values, PlayerMetricCode.RECOVERIES),
                    intValue(values, PlayerMetricCode.MID_DUELS_WON),
                    intValue(values, PlayerMetricCode.MID_SUCCESSFUL_DRIBBLES),
                    intValue(values, PlayerMetricCode.MID_SHOTS));
        }

        PlayerHistoryMetricsDto.Forward forward = null;
        if (hasGroup(values, PositionGroup.FORWARD)) {
            forward = new PlayerHistoryMetricsDto.Forward(
                    intValue(values, PlayerMetricCode.FWD_GOALS),
                    intValue(values, PlayerMetricCode.FWD_ASSISTS),
                    intValue(values, PlayerMetricCode.FWD_SHOTS),
                    intValue(values, PlayerMetricCode.FWD_SUCCESSFUL_DRIBBLES),
                    intValue(values, PlayerMetricCode.CHANCES_CREATED),
                    intValue(values, PlayerMetricCode.CROSSES_ATTEMPTED),
                    intValue(values, PlayerMetricCode.CROSSES_COMPLETED));
        }

        if (goalkeeper == null && defender == null && midfielder == null && forward == null) {
            return null;
        }
        return new PlayerHistoryMetricsDto(goalkeeper, defender, midfielder, forward);
    }

    private boolean hasGroup(Map<String, BigDecimal> values, PositionGroup group) {
        return List.of(PlayerMetricCode.values()).stream()
                .filter(code -> code.group() == group)
                .anyMatch(code -> values.containsKey(code.code()));
    }

    private Integer intValue(Map<String, BigDecimal> values, PlayerMetricCode code) {
        BigDecimal value = values.get(code.code());
        return value == null ? null : value.intValue();
    }

    private Boolean booleanValue(Map<String, BigDecimal> values, PlayerMetricCode code) {
        BigDecimal value = values.get(code.code());
        return value == null ? null : value.compareTo(BigDecimal.ONE) == 0;
    }
}
