package com.verityai.obligation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ObligationRepository extends JpaRepository<Obligation, Long>, JpaSpecificationExecutor<Obligation> {
    @Override
    @EntityGraph(attributePaths = "assessment")
    Optional<Obligation> findById(Long id);

    @EntityGraph(attributePaths = "assessment")
    List<Obligation> findByAssessmentIdOrderByCreatedAtDesc(Long assessmentId);

    long countByAssessmentId(Long assessmentId);

    @Query("""
            select o.assessment.id, o.riskLevel, count(o)
            from Obligation o
            where o.assessment.id in :ids
            group by o.assessment.id, o.riskLevel
            """)
    List<Object[]> countByRisk(@Param("ids") Collection<Long> ids);

}
