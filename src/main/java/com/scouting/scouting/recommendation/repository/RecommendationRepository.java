package com.scouting.scouting.recommendation.repository;

import com.scouting.scouting.recommendation.entity.Recommendation;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    boolean existsByProspect_IdAndClub_Id(Long prospectId, Long clubId);

    Optional<Recommendation> findByProspect_IdAndClub_Id(Long prospectId, Long clubId);

    @EntityGraph(attributePaths = {"prospect", "prospect.player", "prospect.player.user", "scout", "club"})
    Page<Recommendation> findByProspect_IdAndScout_Id(Long prospectId, Long scoutId, Pageable pageable);

    @EntityGraph(attributePaths = {"prospect", "prospect.player", "prospect.player.user", "scout", "club"})
    Page<Recommendation> findByClub_Id(Long clubId, Pageable pageable);

    @EntityGraph(attributePaths = {"prospect", "prospect.player", "prospect.player.user", "scout", "club"})
    Page<Recommendation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"prospect", "prospect.player", "prospect.player.user", "scout", "club"})
    Optional<Recommendation> findByIdAndScout_Id(Long id, Long scoutId);

    @EntityGraph(attributePaths = {"prospect", "prospect.player", "prospect.player.user", "scout", "club"})
    Optional<Recommendation> findByIdAndClub_Id(Long id, Long clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"prospect", "prospect.player", "prospect.player.user", "scout", "club"})
    @Query("select r from Recommendation r where r.id = :id and r.club.id = :clubId")
    Optional<Recommendation> findByIdAndClub_IdForUpdate(@Param("id") Long id, @Param("clubId") Long clubId);
}
