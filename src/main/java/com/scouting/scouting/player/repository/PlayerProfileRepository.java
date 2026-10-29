package com.scouting.scouting.player.repository;

import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.player.entity.PlayerOperationalStatus;
import com.scouting.scouting.player.entity.PlayerPosition;
import com.scouting.scouting.player.entity.PlayerProfile;
import com.scouting.scouting.player.entity.PlayerProfileStatus;
import com.scouting.scouting.user.entity.Role;
import com.scouting.scouting.user.entity.User;
import com.scouting.scouting.user.entity.UserStatus;
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
import java.util.Optional;

public interface PlayerProfileRepository extends JpaRepository<PlayerProfile, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<PlayerProfile> findByUser_Id(Long userId);

    @EntityGraph(attributePaths = "user")
    @Query("select p from PlayerProfile p where p.id = :id")
    Optional<PlayerProfile> findDetailById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    @Query("select p from PlayerProfile p where p.id = :id")
    Optional<PlayerProfile> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "user")
    @Query("select p from PlayerProfile p where p.user.id = :userId")
    Optional<PlayerProfile> findByUser_IdForUpdate(@Param("userId") Long userId);

    boolean existsByUser_Id(Long userId);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select p from PlayerProfile p
            where (:qPattern is null
                   or lower(p.user.firstName) like lower(:qPattern) escape '!'
                   or lower(p.user.lastName) like lower(:qPattern) escape '!')
              and p.position in :positions
              and (:minBirthDate is null or p.birthDate <= :minBirthDate)
              and (:maxBirthDateExclusive is null or p.birthDate > :maxBirthDateExclusive)
              and (:residencePrefix is null or p.residenceUbigeoCode like concat(:residencePrefix, '%'))
              and (:academyId is null or exists (
                    select uo.id from UserOrganization uo
                    where uo.user = p.user and uo.academy.id = :academyId))
              and (:scopedAcademyId is null or exists (
                    select uo2.id from UserOrganization uo2
                    where uo2.user = p.user and uo2.academy.id = :scopedAcademyId))
              and (:operationalStatus is null or p.operationalStatus = :operationalStatus)
              and (:profileStatus is null or p.profileStatus = :profileStatus)
              and (:requireActiveUser = false or p.user.status = :userStatus)
              and (:requireActiveAcademy = false or exists (
                    select uo3.id from UserOrganization uo3
                    where uo3.user = p.user and uo3.academy.operationalStatus = :academyStatus))
            """)
    Page<PlayerProfile> search(
            @Param("qPattern") String qPattern,
            @Param("positions") Collection<PlayerPosition> positions,
            @Param("minBirthDate") LocalDate minBirthDate,
            @Param("maxBirthDateExclusive") LocalDate maxBirthDateExclusive,
            @Param("residencePrefix") String residencePrefix,
            @Param("academyId") Long academyId,
            @Param("scopedAcademyId") Long scopedAcademyId,
            @Param("operationalStatus") PlayerOperationalStatus operationalStatus,
            @Param("profileStatus") PlayerProfileStatus profileStatus,
            @Param("requireActiveUser") boolean requireActiveUser,
            @Param("userStatus") UserStatus userStatus,
            @Param("requireActiveAcademy") boolean requireActiveAcademy,
            @Param("academyStatus") OperationalStatus academyStatus,
            Pageable pageable);

    @Query("""
            select u from User u
            where u.role = :role
              and u.status = :status
              and (:academyId is null or exists (
                    select uo.id from UserOrganization uo
                    where uo.user = u and uo.academy.id = :academyId))
              and not exists (select p.id from PlayerProfile p where p.user = u)
              and (:qPattern is null
                   or lower(u.firstName) like lower(:qPattern) escape '!'
                   or lower(u.lastName) like lower(:qPattern) escape '!')
            """)
    Page<User> searchAccountCandidates(
            @Param("role") Role role,
            @Param("status") UserStatus status,
            @Param("academyId") Long academyId,
            @Param("qPattern") String qPattern,
            Pageable pageable);
}
