package com.verityai.assessment;

import com.verityai.common.AnalysisMode;
import com.verityai.obligation.ObligationResponse;

import java.util.List;

public record AnalyzeResponse(
        AssessmentResponse assessment,
        List<ObligationResponse> obligations,
        AnalysisMode analysisMode,
        String message
) {
}
