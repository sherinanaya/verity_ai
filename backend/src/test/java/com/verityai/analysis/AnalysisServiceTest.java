package com.verityai.analysis;

import com.verityai.common.AnalysisMode;
import com.verityai.common.RiskLevel;
import com.verityai.document.ExtractedDocument;
import com.verityai.document.PageText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {
    private final ExtractedDocument document = new ExtractedDocument(
            "policy.pdf",
            1200,
            2,
            List.of(
                    new PageText(1, "Employees must report a security incident within 24 hours to the security lead."),
                    new PageText(2, "Passwords should be changed regularly when it is operationally convenient.")
            ));

    @Test
    void usesRuleFallbackWhenNoApiKeyIsConfigured() {
        AnalysisService service = new AnalysisService(
                new OpenAiObligationExtractor("", "gpt-4o-mini"),
                new RuleBasedObligationExtractor());

        AnalysisResult result = service.analyze(document);

        assertEquals(AnalysisMode.RULE_BASED, result.mode());
        assertFalse(result.drafts().isEmpty());
        assertTrue(result.drafts().stream().anyMatch(draft -> draft.getSourcePage() != null && draft.getSourcePage() == 1));
        assertTrue(result.drafts().stream().allMatch(draft -> draft.getSourceExcerpt() != null && !draft.getSourceExcerpt().isBlank()));
    }

    @Test
    void dropsFabricatedEvidenceAndKeepsTheFindingAsAnInterpretation() throws Exception {
        OpenAiObligationExtractor openAi = mock(OpenAiObligationExtractor.class);
        when(openAi.isConfigured()).thenReturn(true);
        ObligationDraft invented = new ObligationDraft();
        invented.setRequirement("Potential gap: the policy does not name a retention period.");
        invented.setCategory("Retention");
        invented.setRiskLevel(RiskLevel.MEDIUM);
        invented.setSourcePage(9);
        invented.setSourceExcerpt("Records shall be retained for seven years.");
        invented.setInferred(false);
        when(openAi.extract(any())).thenReturn(List.of(invented));

        AnalysisResult result = new AnalysisService(openAi, new RuleBasedObligationExtractor()).analyze(document);

        assertEquals(AnalysisMode.AI, result.mode());
        ObligationDraft saved = result.drafts().get(0);
        assertNull(saved.getSourcePage());
        assertTrue(saved.getSourceExcerpt().isBlank());
        assertTrue(saved.isInferred());
    }

    @Test
    void fallsBackWhenTheModelCallFails() throws Exception {
        OpenAiObligationExtractor openAi = mock(OpenAiObligationExtractor.class);
        when(openAi.isConfigured()).thenReturn(true);
        when(openAi.extract(any())).thenThrow(new IllegalStateException("unavailable"));

        AnalysisResult result = new AnalysisService(openAi, new RuleBasedObligationExtractor()).analyze(document);

        assertEquals(AnalysisMode.RULE_BASED, result.mode());
        assertTrue(result.message().contains("rule-based"));
    }
}
