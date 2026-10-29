package com.scouting.scouting.recommendation.repository;

import com.scouting.scouting.recommendation.entity.RecommendationEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationEvaluationRepository extends JpaRepository<RecommendationEvaluation, Long> {
}
