package com.verityai.assessment;

import com.verityai.analysis.AnalysisResult;
import com.verityai.analysis.ObligationDraft;
import com.verityai.common.NotFoundException;
import com.verityai.common.RiskLevel;
import com.verityai.common.TextLimits;
import com.verityai.document.ExtractedDocument;
import com.verityai.obligation.Obligation;
import com.verityai.obligation.ObligationRepository;
import com.verityai.obligation.ObligationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AssessmentService {
    private final AssessmentRepository assessments;
    private final ObligationRepository obligations;

    public AssessmentService(AssessmentRepository assessments, ObligationRepository obligations) {
        this.assessments = assessments;
        this.obligations = obligations;
    }

    @Transactional
    public AnalyzeResponse record(ExtractedDocument document, AnalysisResult result) {
        Assessment assessment = new Assessment();
        assessment.setFileName(TextLimits.clip(document.fileName(), 255));
        assessment.setAnalysisMode(result.mode());
        assessment.setPageCount(document.totalPages());
        assessment.setFileSizeBytes(document.sizeBytes());
        assessment.setSummaryMessage(TextLimits.clip(result.message(), 1000));
        assessment.setObligationCount(result.drafts().size());
        Assessment savedAssessment = assessments.save(assessment);

        List<Obligation> saved = result.drafts().stream()
                .map(draft -> obligations.save(toEntity(draft, savedAssessment)))
                .toList();
        Map<RiskLevel, Long> counts = countsFrom(saved);
        return new AnalyzeResponse(
                AssessmentResponse.from(savedAssessment, counts),
                saved.stream().map(ObligationResponse::from).toList(),
                result.mode(),
                result.message());
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponse> list() {
        List<Assessment> all = assessments.findAllByOrderByCreatedAtDesc();
        Map<Long, Map<RiskLevel, Long>> grouped = riskCounts(all.stream().map(Assessment::getId).toList());
        return all.stream()
                .map(assessment -> AssessmentResponse.from(
                        assessment,
                        grouped.getOrDefault(assessment.getId(), Map.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AssessmentDetailResponse get(Long id) {
        Assessment assessment = assessments.findById(id)
                .orElseThrow(() -> new NotFoundException("Assessment not found."));
        List<Obligation> items = obligations.findByAssessmentIdOrderByCreatedAtDesc(id);
        return new AssessmentDetailResponse(
                AssessmentResponse.from(assessment, countsFrom(items)),
                items.stream().map(ObligationResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public List<ObligationResponse> obligationsFor(Long id) {
        if (!assessments.existsById(id)) {
            throw new NotFoundException("Assessment not found.");
        }
        return obligations.findByAssessmentIdOrderByCreatedAtDesc(id).stream()
                .map(ObligationResponse::from)
                .toList();
    }

    private Map<Long, Map<RiskLevel, Long>> riskCounts(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Map<RiskLevel, Long>> grouped = new HashMap<>();
        for (Object[] row : obligations.countByRisk(ids)) {
            Long assessmentId = (Long) row[0];
            RiskLevel level = (RiskLevel) row[1];
            if (assessmentId == null || level == null) {
                continue;
            }
            long count = ((Number) row[2]).longValue();
            grouped.computeIfAbsent(assessmentId, key -> new EnumMap<>(RiskLevel.class)).put(level, count);
        }
        return grouped;
    }

    private static Map<RiskLevel, Long> countsFrom(List<Obligation> items) {
        Map<RiskLevel, Long> counts = new EnumMap<>(RiskLevel.class);
        for (Obligation item : items) {
            if (item.getRiskLevel() != null) {
                counts.merge(item.getRiskLevel(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private static Obligation toEntity(ObligationDraft draft, Assessment assessment) {
        Obligation obligation = new Obligation();
        obligation.setAssessment(assessment);
        obligation.setRequirement(TextLimits.clip(draft.getRequirement(), 2000));
        obligation.setCategory(TextLimits.clip(draft.getCategory(), 120));
        obligation.setRiskLevel(draft.getRiskLevel() == null ? RiskLevel.MEDIUM : draft.getRiskLevel());
        obligation.setStatus(com.verityai.common.ReviewStatus.NEEDS_REVIEW);
        obligation.setSourcePage(draft.getSourcePage());
        obligation.setSourceExcerpt(TextLimits.clip(draft.getSourceExcerpt(), 2000));
        obligation.setRationale(TextLimits.clip(draft.getRationale(), 2000));
        obligation.setRecommendation(TextLimits.clip(draft.getRecommendation(), 2000));
        obligation.setInferred(draft.isInferred());
        return obligation;
    }
}
