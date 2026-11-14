package com.verityai.analysis;

import com.verityai.common.AnalysisFailedException;
import com.verityai.common.AnalysisMode;
import com.verityai.document.ExtractedDocument;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class AnalysisService {
    private final OpenAiObligationExtractor openAi;
    private final RuleBasedObligationExtractor rules;

    public AnalysisService(OpenAiObligationExtractor openAi, RuleBasedObligationExtractor rules) {
        this.openAi = openAi;
        this.rules = rules;
    }

    public AnalysisResult analyze(ExtractedDocument document) {
        if (openAi.isConfigured()) {
            try {
                List<ObligationDraft> drafts = prepare(openAi.extract(document), document);
                if (!drafts.isEmpty()) {
                    return new AnalysisResult(
                            AnalysisMode.AI,
                            "AI analysis completed. These are candidate findings for human review, not a compliance determination.",
                            drafts);
                }
            } catch (Exception ex) {
                openAi.logFailure(ex);
            }
            List<ObligationDraft> fallback = prepare(rules.extract(document), document);
            if (fallback.isEmpty()) {
                throw new AnalysisFailedException(
                        "No obligations could be extracted. Try a more detailed policy document.");
            }
            return new AnalysisResult(
                    AnalysisMode.RULE_BASED,
                    "AI analysis did not return usable findings, so rule-based extraction was used. Review each finding against the source text. This is not a legal determination.",
                    fallback);
        }
        List<ObligationDraft> drafts = prepare(rules.extract(document), document);
        if (drafts.isEmpty()) {
            throw new AnalysisFailedException(
                    "No obligations could be extracted. Try a more detailed policy document.");
        }
        return new AnalysisResult(
                AnalysisMode.RULE_BASED,
                "Rule-based extraction was used because GROQ_API_KEY is not configured. Review each finding against the source text. This is not a legal determination.",
                drafts);
    }

    private List<ObligationDraft> prepare(List<ObligationDraft> extracted, ExtractedDocument document) {
        List<ObligationDraft> prepared = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ObligationDraft draft : extracted) {
            if (draft.getRequirement() == null || draft.getRequirement().isBlank()) {
                continue;
            }
            EvidenceChecker.reconcile(draft, document.pages());
            String key = draft.getRequirement().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
            if (!seen.add(key)) {
                continue;
            }
            if (draft.getRecommendation() == null || draft.getRecommendation().isBlank()) {
                draft.setRecommendation("Assign an owner, confirm the finding applies, and record the evidence a reviewer would inspect.");
            }
            if (draft.getCategory() == null || draft.getCategory().isBlank()) {
                draft.setCategory("General Governance");
            }
            prepared.add(draft);
            if (prepared.size() == 12) {
                break;
            }
        }
        return prepared;
    }
}
