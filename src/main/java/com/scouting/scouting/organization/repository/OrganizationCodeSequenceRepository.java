package com.scouting.scouting.organization.repository;

import com.scouting.scouting.organization.entity.OrganizationCodeSequence;
import com.scouting.scouting.organization.entity.OrganizationType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrganizationCodeSequenceRepository extends JpaRepository<OrganizationCodeSequence, OrganizationType> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from OrganizationCodeSequence s where s.type = :type")
    Optional<OrganizationCodeSequence> findForUpdate(@Param("type") OrganizationType type);
}
