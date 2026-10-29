package com.scouting.scouting.player.repository;

import com.scouting.scouting.player.entity.ActivityType;
import com.scouting.scouting.player.entity.AgeCategory;
import com.scouting.scouting.player.entity.CompetitiveLevel;
import com.scouting.scouting.player.entity.PlayerHistoryEntry;
import com.scouting.scouting.player.entity.PlayerPosition;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlayerHistoryEntryRepository extends JpaRepository<PlayerHistoryEntry, Long> {

    @EntityGraph(attributePaths = {"playerProfile", "createdBy", "lastModifiedBy"})
    List<PlayerHistoryEntry> findByPlayerProfile_IdOrderByActivityDateDescIdDesc(Long playerProfileId);

    @EntityGraph(attributePaths = {"playerProfile", "createdBy", "lastModifiedBy"})
    Optional<PlayerHistoryEntry> findByIdAndPlayerProfile_Id(Long id, Long playerProfileId);

    @EntityGraph(attributePaths = {"metrics"})
    List<PlayerHistoryEntry> findByIdInAndPlayerProfile_Id(Collection<Long> ids, Long playerProfileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"playerProfile", "createdBy", "lastModifiedBy"})
    @Query("select e from PlayerHistoryEntry e where e.id = :id and e.playerProfile.id = :profileId")
    Optional<PlayerHistoryEntry> findByIdAndPlayerProfile_IdForUpdate(
            @Param("id") Long id, @Param("profileId") Long profileId);

    @EntityGraph(attributePaths = {"createdBy", "lastModifiedBy"})
    @Query("""
            select e from PlayerHistoryEntry e
            where e.playerProfile.id = :profileId
              and (:activityType is null or e.activityType = :activityType)
              and (:competitiveLevel is null or e.competitiveLevel = :competitiveLevel)
              and (:ageCategory is null or e.ageCategory = :ageCategory)
              and (:position is null or e.positionPlayed = :position)
              and (:year is null or year(e.activityDate) = :year)
              and (:fromDate is null or e.activityDate >= :fromDate)
              and (:toDate is null or e.activityDate <= :toDate)
            """)
    Page<PlayerHistoryEntry> search(
            @Param("profileId") Long profileId,
            @Param("activityType") ActivityType activityType,
            @Param("competitiveLevel") CompetitiveLevel competitiveLevel,
            @Param("ageCategory") AgeCategory ageCategory,
            @Param("position") PlayerPosition position,
            @Param("year") Integer year,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query("select min(e.activityDate) from PlayerHistoryEntry e where e.playerProfile.id = :profileId")
    LocalDate findFirstActivityDate(@Param("profileId") Long profileId);

    @Query("""
            select e.playerProfile.id as playerId, min(e.activityDate) as firstActivityDate
            from PlayerHistoryEntry e
            where e.playerProfile.id in :profileIds
            group by e.playerProfile.id
            """)
    List<PlayerFirstActivityProjection> findFirstActivityDates(@Param("profileIds") Collection<Long> profileIds);

    interface PlayerFirstActivityProjection {
        Long getPlayerId();

        LocalDate getFirstActivityDate();
    }
}
