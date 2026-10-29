package com.scouting.scouting.player.repository;

import com.scouting.scouting.player.entity.PlayerGuardian;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerGuardianRepository extends JpaRepository<PlayerGuardian, Long> {

    @EntityGraph(attributePaths = {"playerProfile", "createdBy", "updatedBy"})
    Optional<PlayerGuardian> findByPlayerProfile_Id(Long playerProfileId);

    boolean existsByPlayerProfile_Id(Long playerProfileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"playerProfile", "createdBy", "updatedBy"})
    @Query("select g from PlayerGuardian g where g.playerProfile.id = :profileId")
    Optional<PlayerGuardian> findByPlayerProfile_IdForUpdate(@Param("profileId") Long profileId);
}
