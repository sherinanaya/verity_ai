package com.verityai.assessment;

import com.verityai.common.AnalysisMode;
import com.verityai.common.RiskLevel;
import com.verityai.common.TextLimits;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record AssessmentResponse(
        Long id,
        String fileName,
        AnalysisMode analysisMode,
        Integer pageCount,
        Long fileSizeBytes,
        int obligationCount,
        String summaryMessage,
        Map<String, Long> riskSummary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AssessmentResponse from(Assessment assessment, Map<RiskLevel, Long> counts) {
        Map<String, Long> summary = new LinkedHashMap<>();
        Map<RiskLevel, Long> safeCounts = counts == null ? Map.of() : counts;
        int total = 0;
        for (RiskLevel level : RiskLevel.values()) {
            long count = safeCounts.getOrDefault(level, 0L);
            summary.put(level.name(), count);
            total += (int) count;
        }
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getFileName(),
                assessment.getAnalysisMode(),
                assessment.getPageCount(),
                assessment.getFileSizeBytes(),
                total,
                TextLimits.emptyToNull(assessment.getSummaryMessage()),
                summary,
                assessment.getCreatedAt(),
                assessment.getUpdatedAt());
    }
}
