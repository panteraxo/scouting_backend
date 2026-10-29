package com.scouting.scouting.prospecting.repository;

import com.scouting.scouting.prospecting.entity.ProspectEvaluation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProspectEvaluationRepository extends JpaRepository<ProspectEvaluation, Long> {

    @EntityGraph(attributePaths = {"createdBy"})
    Page<ProspectEvaluation> findByProspect_Id(Long prospectId, Pageable pageable);

    @EntityGraph(attributePaths = {"createdBy"})
    Optional<ProspectEvaluation> findByIdAndProspect_Id(Long id, Long prospectId);

    @EntityGraph(attributePaths = {"createdBy"})
    java.util.List<ProspectEvaluation> findByIdInAndProspect_Id(
            java.util.Collection<Long> ids, Long prospectId);
}
