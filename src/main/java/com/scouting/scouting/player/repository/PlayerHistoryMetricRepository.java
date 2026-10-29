package com.scouting.scouting.player.repository;

import com.scouting.scouting.player.entity.PlayerHistoryMetric;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerHistoryMetricRepository extends JpaRepository<PlayerHistoryMetric, Long> {
}
