package com.verityai.obligation;

import com.verityai.assessment.Assessment;
import com.verityai.assessment.AssessmentRepository;
import com.verityai.common.NotFoundException;
import com.verityai.common.ReviewStatus;
import com.verityai.common.RiskLevel;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ObligationService {
    private final ObligationRepository obligations;
    private final AssessmentRepository assessments;

    public ObligationService(ObligationRepository obligations, AssessmentRepository assessments) {
        this.obligations = obligations;
        this.assessments = assessments;
    }

    @Transactional(readOnly = true)
    public List<ObligationResponse> list(Long assessmentId, RiskLevel risk, ReviewStatus status, String query) {
        String q = query == null || query.isBlank() ? null : query.trim();
        return obligations.findAll(matches(assessmentId, risk, status, q)).stream()
                .map(ObligationResponse::from)
                .toList();
    }

    private static Specification<Obligation> matches(Long assessmentId, RiskLevel risk, ReviewStatus status, String query) {
        return (root, criteria, cb) -> {
            Class<?> resultType = criteria.getResultType();
            if (resultType == null || (!Long.class.equals(resultType) && !long.class.equals(resultType))) {
                root.fetch("assessment", JoinType.LEFT);
                criteria.distinct(true);
            }
            List<Predicate> predicates = new ArrayList<>();
            if (assessmentId != null) {
                predicates.add(cb.equal(root.get("assessment").get("id"), assessmentId));
            }
            if (risk != null) {
                predicates.add(cb.equal(root.get("riskLevel"), risk));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (query != null) {
                String like = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("requirement")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("category"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("assessment").get("fileName"), "")), like)
                ));
            }
            criteria.orderBy(cb.desc(root.get("createdAt")));
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public ObligationResponse get(Long id) {
        return ObligationResponse.from(find(id));
    }

    @Transactional
    public ObligationResponse updateStatus(Long id, ReviewStatus status) {
        Obligation obligation = find(id);
        obligation.setStatus(status);
        return ObligationResponse.from(obligations.save(obligation));
    }

    @Transactional
    public void delete(Long id) {
        Obligation obligation = find(id);
        Assessment assessment = obligation.getAssessment();
        obligations.delete(obligation);
        obligations.flush();
        if (assessment != null && assessment.getId() != null) {
            int remaining = (int) obligations.countByAssessmentId(assessment.getId());
            assessment.setObligationCount(remaining);
            assessments.save(assessment);
        }
    }

    private Obligation find(Long id) {
        return obligations.findById(id).orElseThrow(() -> new NotFoundException("Obligation not found."));
    }
}
