package com.scouting.scouting.recommendation.repository;

import com.scouting.scouting.recommendation.entity.RecommendationHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationHistoryEntryRepository extends JpaRepository<RecommendationHistoryEntry, Long> {
}
