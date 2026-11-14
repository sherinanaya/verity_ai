package com.verityai.assessment;

import com.verityai.obligation.ObligationResponse;

import java.util.List;

public record AssessmentDetailResponse(
        AssessmentResponse assessment,
        List<ObligationResponse> obligations
) {
}
