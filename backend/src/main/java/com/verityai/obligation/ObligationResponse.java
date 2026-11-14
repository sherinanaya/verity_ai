package com.verityai.obligation;

import com.verityai.assessment.Assessment;
import com.verityai.common.RiskLevel;
import com.verityai.common.ReviewStatus;
import com.verityai.common.TextLimits;

import java.time.LocalDateTime;

public record ObligationResponse(
        Long id,
        Long assessmentId,
        String sourceDocument,
        String requirement,
        String category,
        RiskLevel riskLevel,
        ReviewStatus status,
        Integer sourcePage,
        String sourceExcerpt,
        String rationale,
        String recommendation,
        boolean inferred,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ObligationResponse from(Obligation obligation) {
        Assessment assessment = obligation.getAssessment();
        return new ObligationResponse(
                obligation.getId(),
                assessment == null ? null : assessment.getId(),
                assessment == null ? null : assessment.getFileName(),
                obligation.getRequirement(),
                obligation.getCategory(),
                obligation.getRiskLevel(),
                obligation.getStatus(),
                obligation.getSourcePage(),
                TextLimits.emptyToNull(obligation.getSourceExcerpt()),
                TextLimits.emptyToNull(obligation.getRationale()),
                TextLimits.emptyToNull(obligation.getRecommendation()),
                obligation.isInferred(),
                obligation.getCreatedAt(),
                obligation.getUpdatedAt());
    }
}
