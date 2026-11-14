package com.verityai.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiObligationExtractorTest {
    @Test
    void parsesStrictJsonAndIgnoresUnknownRiskLabels() throws Exception {
        String json = """
                {"obligations":[
                  {"requirement":"Staff must log access.","category":"Access Control","riskLevel":"EXTREME","sourcePage":2,"sourceExcerpt":"Staff must log access.","rationale":"Review.","recommendation":"Assign an owner.","inferred":false}
                ]}
                """;

        List<ObligationDraft> drafts = OpenAiObligationExtractor.parseContent(json, new ObjectMapper());

        assertEquals(1, drafts.size());
        assertEquals(com.verityai.common.RiskLevel.MEDIUM, drafts.get(0).getRiskLevel());
        assertEquals(2, drafts.get(0).getSourcePage());
    }

    @Test
    void stripsMarkdownFences() {
        assertTrue(OpenAiObligationExtractor.stripFence("```json\n{\"obligations\":[]}\n```").startsWith("{"));
    }
}
