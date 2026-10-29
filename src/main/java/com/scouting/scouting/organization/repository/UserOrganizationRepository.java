package com.scouting.scouting.organization.repository;

import com.scouting.scouting.organization.entity.UserOrganization;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> {

    @EntityGraph(attributePaths = {"academy", "club"})
    Optional<UserOrganization> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"academy", "club"})
    List<UserOrganization> findByUser_IdIn(Collection<Long> userIds);

    @EntityGraph(attributePaths = "user")
    List<UserOrganization> findByClub_Id(Long clubId);

    @EntityGraph(attributePaths = "user")
    List<UserOrganization> findByAcademy_Id(Long academyId);
}
