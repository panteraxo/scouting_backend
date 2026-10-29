package com.scouting.scouting.player.repository;

import com.scouting.scouting.player.entity.PlayerHistoryRevision;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerHistoryRevisionRepository extends JpaRepository<PlayerHistoryRevision, Long> {

    List<PlayerHistoryRevision> findByHistoryEntry_IdOrderByRevisionNumberDesc(Long historyEntryId);

    Page<PlayerHistoryRevision> findByHistoryEntry_Id(Long historyEntryId, Pageable pageable);
}
