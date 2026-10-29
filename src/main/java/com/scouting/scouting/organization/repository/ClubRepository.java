package com.scouting.scouting.organization.repository;

import com.scouting.scouting.organization.entity.Club;
import com.scouting.scouting.organization.entity.OperationalStatus;
import com.scouting.scouting.organization.entity.VerificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClubRepository extends JpaRepository<Club, Long> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Club c where c.id = :id")
    Optional<Club> findByIdForUpdate(@Param("id") Long id);

    /**
     * {@code qPattern} llega ya escapado y envuelto en {@code %} por el
     * servicio; {@code %}, {@code _} y {@code !} se buscan literalmente.
     */
    @Query("""
            select c from Club c
            where (:qPattern is null
                   or lower(c.name) like lower(:qPattern) escape '!'
                   or lower(c.code) like lower(:qPattern) escape '!')
              and (:type is null or lower(c.type) = lower(:type))
              and (:ubigeoPrefix is null or c.ubigeoCode like concat(:ubigeoPrefix, '%'))
              and (:operationalStatus is null or c.operationalStatus = :operationalStatus)
              and (:verificationStatus is null or c.verificationStatus = :verificationStatus)
            """)
    Page<Club> searchAdmin(
            @Param("qPattern") String qPattern,
            @Param("type") String type,
            @Param("ubigeoPrefix") String ubigeoPrefix,
            @Param("operationalStatus") OperationalStatus operationalStatus,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            Pageable pageable);

    @Query("""
            select c from Club c
            where c.operationalStatus = :operationalStatus
              and c.verificationStatus = :verificationStatus
              and (:qPattern is null
                   or lower(c.name) like lower(:qPattern) escape '!'
                   or lower(c.code) like lower(:qPattern) escape '!')
              and (:type is null or lower(c.type) = lower(:type))
              and (:ubigeoPrefix is null or c.ubigeoCode like concat(:ubigeoPrefix, '%'))
            """)
    Page<Club> searchDirectory(
            @Param("operationalStatus") OperationalStatus operationalStatus,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("qPattern") String qPattern,
            @Param("type") String type,
            @Param("ubigeoPrefix") String ubigeoPrefix,
            Pageable pageable);

    Optional<Club> findByIdAndOperationalStatusAndVerificationStatus(
            Long id, OperationalStatus operationalStatus, VerificationStatus verificationStatus);
}
