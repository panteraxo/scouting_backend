package com.scouting.scouting.prospecting.repository;

import com.scouting.scouting.prospecting.entity.Prospect;
import com.scouting.scouting.prospecting.entity.ProspectStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProspectRepository extends JpaRepository<Prospect, Long> {

    @EntityGraph(attributePaths = {"player", "player.user"})
    Optional<Prospect> findByScout_IdAndId(Long scoutId, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"player", "player.user"})
    @Query("select p from Prospect p where p.scout.id = :scoutId and p.id = :id")
    Optional<Prospect> findByScout_IdAndIdForUpdate(
            @Param("scoutId") Long scoutId, @Param("id") Long id);

    boolean existsByScout_IdAndPlayer_Id(Long scoutId, Long playerId);

    @EntityGraph(attributePaths = {"player", "player.user"})
    @Query("""
            select p from Prospect p
            where p.scout.id = :scoutId
              and (:status is null or p.status = :status)
              and (:playerId is null or p.player.id = :playerId)
              and (:qPattern is null
                   or lower(p.player.user.firstName) like lower(:qPattern) escape '!'
                   or lower(p.player.user.lastName) like lower(:qPattern) escape '!')
            """)
    Page<Prospect> searchOwn(
            @Param("scoutId") Long scoutId,
            @Param("status") ProspectStatus status,
            @Param("playerId") Long playerId,
            @Param("qPattern") String qPattern,
            Pageable pageable);
}
