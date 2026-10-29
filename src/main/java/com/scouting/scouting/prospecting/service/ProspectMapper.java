package com.scouting.scouting.prospecting.service;

import com.scouting.scouting.organization.entity.Academy;
import com.scouting.scouting.organization.entity.UserOrganization;
import com.scouting.scouting.player.dto.AcademyRefResponse;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.service.PlayerTime;
import com.scouting.scouting.prospecting.dto.ProspectEvaluationResponse;
import com.scouting.scouting.prospecting.dto.ProspectPlayerSummaryResponse;
import com.scouting.scouting.prospecting.dto.ProspectResponse;
import com.scouting.scouting.prospecting.entity.Prospect;
import com.scouting.scouting.prospecting.entity.ProspectEvaluation;
import org.springframework.stereotype.Component;

/** Conversion explicita de entidades SCP-006 a DTOs sin PII adicional. */
@Component
public class ProspectMapper {

    public ProspectPlayerSummaryResponse playerSummary(PlayerProfile player, UserOrganization organization) {
        Academy academy = organization == null ? null : organization.getAcademy();
        return new ProspectPlayerSummaryResponse(
                player.getId(),
                player.getUser().getFirstName(),
                player.getUser().getLastName(),
                player.getPhotoUrl(),
                PlayerTime.age(player.getBirthDate(), PlayerTime.today()),
                player.getPosition(),
                player.getPosition() == null ? null : player.getPosition().group(),
                academy == null ? null : new AcademyRefResponse(
                        academy.getId(),
                        academy.getCode(),
                        academy.getName(),
                        academy.getLogoUrl(),
                        academy.getOperationalStatus(),
                        academy.getVerificationStatus()));
    }

    public ProspectResponse toResponse(Prospect prospect, UserOrganization organization) {
        return new ProspectResponse(
                prospect.getId(),
                playerSummary(prospect.getPlayer(), organization),
                prospect.getStatus(),
                prospect.getPrivateNotes(),
                prospect.getStartedAt(),
                prospect.getLastActivityAt(),
                prospect.getClosedAt(),
                prospect.getCreatedAt(),
                prospect.getUpdatedAt(),
                prospect.getVersion());
    }

    public ProspectEvaluationResponse toEvaluation(ProspectEvaluation evaluation) {
        return new ProspectEvaluationResponse(
                evaluation.getId(),
                evaluation.getProspect().getId(),
                evaluation.getEvaluatedAt(),
                evaluation.getTechnicalRating(),
                evaluation.getTacticalRating(),
                evaluation.getPhysicalRating(),
                evaluation.getMentalRating(),
                evaluation.getOverallRating(),
                evaluation.getStrengths(),
                evaluation.getImprovements(),
                evaluation.getRecommendation(),
                evaluation.getPrivateNotes(),
                evaluation.getVideoUrl(),
                evaluation.getCreatedAt());
    }
}
