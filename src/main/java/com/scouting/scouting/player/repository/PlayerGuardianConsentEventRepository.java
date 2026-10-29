package com.scouting.scouting.player.repository;

import com.scouting.scouting.player.entity.PlayerGuardianConsentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerGuardianConsentEventRepository extends JpaRepository<PlayerGuardianConsentEvent, Long> {

    Optional<PlayerGuardianConsentEvent> findTopByPlayerProfile_IdOrderByRecordedAtDescIdDesc(Long playerProfileId);

    List<PlayerGuardianConsentEvent> findByPlayerProfile_IdOrderByRecordedAtDescIdDesc(Long playerProfileId);
}
