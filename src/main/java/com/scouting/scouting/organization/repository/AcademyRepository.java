package com.scouting.scouting.organization.repository;

import com.scouting.scouting.organization.entity.Academy;
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

public interface AcademyRepository extends JpaRepository<Academy, Long> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Academy a where a.id = :id")
    Optional<Academy> findByIdForUpdate(@Param("id") Long id);

    /**
     * Busqueda administrativa sin restriccion de estado. Los filtros son
     * opcionales y se parametrizan para no interpolar valores del cliente.
     * {@code qPattern} llega ya escapado y envuelto en {@code %} por el
     * servicio; el escape se declara aqui para que {@code %}, {@code _} y
     * {@code !} se traten de forma literal.
     */
    @Query("""
            select a from Academy a
            where (:qPattern is null
                   or lower(a.name) like lower(:qPattern) escape '!'
                   or lower(a.code) like lower(:qPattern) escape '!')
              and (:type is null or lower(a.type) = lower(:type))
              and (:ubigeoPrefix is null or a.ubigeoCode like concat(:ubigeoPrefix, '%'))
              and (:operationalStatus is null or a.operationalStatus = :operationalStatus)
              and (:verificationStatus is null or a.verificationStatus = :verificationStatus)
            """)
    Page<Academy> searchAdmin(
            @Param("qPattern") String qPattern,
            @Param("type") String type,
            @Param("ubigeoPrefix") String ubigeoPrefix,
            @Param("operationalStatus") OperationalStatus operationalStatus,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            Pageable pageable);

    /**
     * Directorio de ojeadores: el servicio siempre pasa ACTIVE/VERIFIED y no
     * existe parametro de estado que pueda ampliar el resultado.
     */
    @Query("""
            select a from Academy a
            where a.operationalStatus = :operationalStatus
              and a.verificationStatus = :verificationStatus
              and (:qPattern is null
                   or lower(a.name) like lower(:qPattern) escape '!'
                   or lower(a.code) like lower(:qPattern) escape '!')
              and (:type is null or lower(a.type) = lower(:type))
              and (:ubigeoPrefix is null or a.ubigeoCode like concat(:ubigeoPrefix, '%'))
            """)
    Page<Academy> searchDirectory(
            @Param("operationalStatus") OperationalStatus operationalStatus,
            @Param("verificationStatus") VerificationStatus verificationStatus,
            @Param("qPattern") String qPattern,
            @Param("type") String type,
            @Param("ubigeoPrefix") String ubigeoPrefix,
            Pageable pageable);

    Optional<Academy> findByIdAndOperationalStatusAndVerificationStatus(
            Long id, OperationalStatus operationalStatus, VerificationStatus verificationStatus);
}
