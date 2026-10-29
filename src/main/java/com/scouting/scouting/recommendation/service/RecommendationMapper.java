package com.scouting.scouting.recommendation.service;

import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.service.PlayerTime;
import com.scouting.scouting.recommendation.dto.RecommendationAdminResponse;
import com.scouting.scouting.recommendation.dto.RecommendationClubRefResponse;
import com.scouting.scouting.recommendation.dto.RecommendationDetailResponse;
import com.scouting.scouting.recommendation.dto.RecommendationPlayerSummaryResponse;
import com.scouting.scouting.recommendation.dto.RecommendationResponseEntry;
import com.scouting.scouting.recommendation.dto.RecommendationScoutRefResponse;
import com.scouting.scouting.recommendation.dto.RecommendationSummaryResponse;
import com.scouting.scouting.recommendation.entity.Recommendation;
import com.scouting.scouting.recommendation.entity.RecommendationResponse;
import com.scouting.scouting.user.entity.User;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

/** Conversion explicita de recomendaciones a DTOs por rol. */
@Component
public class RecommendationMapper {

    private final ObjectMapper objectMapper;

    public RecommendationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RecommendationClubRefResponse club(Club club) {
        return new RecommendationClubRefResponse(
                club.getId(), club.getCode(), club.getName(), club.getLogoUrl(),
                club.getOperationalStatus(), club.getVerificationStatus());
    }

    public RecommendationScoutRefResponse scout(User scout) {
        return new RecommendationScoutRefResponse(scout.getId(), scout.getFirstName(), scout.getLastName());
    }

    public RecommendationPlayerSummaryResponse player(PlayerProfile player) {
        return new RecommendationPlayerSummaryResponse(
                player.getId(),
                player.getUser().getFirstName(),
                player.getUser().getLastName(),
                PlayerTime.age(player.getBirthDate(), PlayerTime.today()),
                player.getPosition(),
                player.getPosition() == null ? null : player.getPosition().group());
    }

    public RecommendationSummaryResponse summary(Recommendation recommendation,
                                                 RecommendationResponse latest) {
        return new RecommendationSummaryResponse(
                recommendation.getId(),
                recommendation.getProspect().getId(),
                player(recommendation.getProspect().getPlayer()),
                scout(recommendation.getScout()),
                club(recommendation.getClub()),
                recommendation.getStatus(),
                recommendation.getSentAt(),
                latest == null ? null : latest.getStatus(),
                latest == null ? null : latest.getRespondedAt());
    }

    public RecommendationDetailResponse detail(Recommendation recommendation,
                                               List<RecommendationResponse> responses) {
        JsonNode snapshot = objectMapper.readTree(recommendation.getSnapshotJson());
        return new RecommendationDetailResponse(
                recommendation.getId(),
                recommendation.getProspect().getId(),
                player(recommendation.getProspect().getPlayer()),
                scout(recommendation.getScout()),
                club(recommendation.getClub()),
                recommendation.getStatus(),
                recommendation.getMessage(),
                snapshot,
                responses.stream().map(this::responseEntry).toList(),
                recommendation.getSentAt(),
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt(),
                recommendation.getVersion());
    }

    public RecommendationResponseEntry responseEntry(RecommendationResponse response) {
        return new RecommendationResponseEntry(
                response.getId(),
                response.getStatus(),
                response.getMessage(),
                response.getRespondedAt());
    }

    public RecommendationAdminResponse admin(Recommendation recommendation,
                                              RecommendationResponse latest) {
        PlayerProfile player = recommendation.getProspect().getPlayer();
        return new RecommendationAdminResponse(
                recommendation.getId(),
                recommendation.getProspect().getId(),
                player.getId(),
                player.getUser().getFirstName(),
                player.getUser().getLastName(),
                recommendation.getScout().getId(),
                recommendation.getScout().getFirstName(),
                recommendation.getScout().getLastName(),
                recommendation.getClub().getId(),
                recommendation.getClub().getCode(),
                recommendation.getClub().getName(),
                recommendation.getStatus(),
                latest == null ? null : latest.getStatus(),
                latest == null ? null : latest.getRespondedAt(),
                recommendation.getSentAt(),
                recommendation.getCreatedAt());
    }
}
