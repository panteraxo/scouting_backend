package com.scouting.scouting.recommendation.repository;

import com.scouting.scouting.recommendation.entity.RecommendationResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecommendationResponseRepository extends JpaRepository<RecommendationResponse, Long> {

    @EntityGraph(attributePaths = {"respondedBy", "club"})
    List<RecommendationResponse> findByRecommendation_IdOrderByRespondedAtDescIdDesc(Long recommendationId);

    @EntityGraph(attributePaths = {"respondedBy", "club"})
    Optional<RecommendationResponse> findTopByRecommendation_IdOrderByRespondedAtDescIdDesc(Long recommendationId);
}
