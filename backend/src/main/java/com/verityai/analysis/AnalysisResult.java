package com.verityai.analysis;

import com.verityai.common.AnalysisMode;

import java.util.List;

public record AnalysisResult(AnalysisMode mode, String message, List<ObligationDraft> drafts) {
}
