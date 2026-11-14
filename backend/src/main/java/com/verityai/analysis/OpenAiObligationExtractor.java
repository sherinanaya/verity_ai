package com.verityai.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verityai.common.RiskLevel;
import com.verityai.document.ExtractedDocument;
import com.verityai.document.PageText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiObligationExtractor {
    private static final Logger log = LoggerFactory.getLogger(OpenAiObligationExtractor.class);
    private static final int MAX_FINDINGS = 12;
    private static final int MAX_PROMPT_CHARS = 18000;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient client;
    private final String apiKey;
    private final String model;

    public OpenAiObligationExtractor(
        @Value("${app.groq-api-key:}") String apiKey,
        @Value("${app.groq-model:openai/gpt-oss-20b}") String model){
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = (model == null || model.isBlank()) ? "openai/gpt-oss-20b" : model.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public List<ObligationDraft> extract(ExtractedDocument document) throws Exception {
        String response = client.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody(document))
                .retrieve()
                .body(String.class);
        if (response == null || response.isBlank()) {
            return List.of();
        }
        JsonNode root = mapper.readTree(response);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        List<ObligationDraft> drafts = parseContent(content, mapper);
        if (drafts.size() > MAX_FINDINGS) {
            return new ArrayList<>(drafts.subList(0, MAX_FINDINGS));
        }
        return drafts;
    }

    
    private Map<String, Object> requestBody(ExtractedDocument document) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("model", model);
        body.put("temperature", 0.1);
        body.put("max_completion_tokens", 4096);

        // Ask Groq to return valid JSON
        body.put("response_format", Map.of(
            "type", "json_object"
        ));

        // Hide internal reasoning from the JSON response
        body.put("reasoning_format", "hidden");
        body.put("reasoning_effort", "low");

        body.put("messages", List.of(
            Map.of("role", "system", "content", systemPrompt()),
            Map.of("role", "user", "content", userPrompt(document))
        ));

        return body;
    }


    
        private static String systemPrompt() {
            return """
                You are a compliance policy analysis engine.
                Extract candidate obligations from the supplied policy.

                Return ONLY a valid JSON object.
                Do not include markdown, code fences, comments,
                explanations, or text outside the JSON.

                The output must follow this structure:
                {
                "obligations": [
                    {
                    "requirement": "string",
                    "category": "Access Control",
                    "riskLevel": "MEDIUM",
                    "sourcePage": 1,
                    "sourceExcerpt": "string",
                    "rationale": "string",
                    "recommendation": "string",
                    "inferred": false
                    }
                ]
                }

                Use valid JSON syntax.
                Escape quotation marks inside strings.
                Do not leave any string or array incomplete.
                If there are no findings, return:
                {"obligations":[]}
                """;
        }


    private static String userPrompt(ExtractedDocument document) {
        return """
                Analyze this organizational policy. Extract up to 12 candidate findings.
                Cover, when the text supports it: regulatory or policy requirements, mandatory duties, reporting obligations, data protection, security controls, deadlines, retention, and missing or ambiguous requirements.
                Return a JSON object with one key, obligations, whose value is an array.
                Each item must use these fields:
                - requirement: string. For a direct duty, restate it conservatively. For a gap or ambiguity, start with "Potential gap:".
                - category: one of Access Control, Data Protection, Incident Management, Audit & Monitoring, Reporting, Retention, Security Controls, General Governance.
                - riskLevel: CRITICAL, HIGH, MEDIUM, or LOW.
                - sourcePage: the integer from a [PAGE n] marker, or null if you cannot point at a page.
                - sourceExcerpt: a short exact quote copied from the document, or an empty string when the finding is an inference with no direct quote.
                - rationale: why a reviewer should look at this. State that human review is required.
                - recommendation: one concrete next action for the reviewer. Do not claim the action is already required by law unless the document says so.
                - inferred: true when the finding is an interpretation or a possible gap, false when it restates explicit document language.
                Do not invent source excerpts or page numbers. If a quote is not present, use an empty excerpt and null page.
                
                DOCUMENT:
                """ + clipped(document);
    }

    static String clipped(ExtractedDocument document) {
        StringBuilder builder = new StringBuilder();
        for (PageText page : document.pages()) {
            String block = "[PAGE " + page.page() + "]\n" + page.text() + "\n\n";
            if (builder.length() + block.length() > MAX_PROMPT_CHARS) {
                break;
            }
            builder.append(block);
        }
        return builder.toString();
    }

    static List<ObligationDraft> parseContent(String content, ObjectMapper mapper) throws Exception {
        String json = stripFence(content);
        if (json.isBlank()) {
            return List.of();
        }
        JsonNode root = mapper.readTree(json);
        JsonNode items = root;
        if (root.isObject() && root.has("obligations")) {
            items = root.get("obligations");
        }
        if (!items.isArray()) {
            return List.of();
        }
        List<ObligationDraft> drafts = new ArrayList<>();
        for (JsonNode node : items) {
            String requirement = node.path("requirement").asText("").trim();
            if (requirement.isBlank()) {
                continue;
            }
            ObligationDraft draft = new ObligationDraft();
            draft.setRequirement(requirement);
            String category = node.path("category").asText("General Governance").trim();
            draft.setCategory(category.isBlank() ? "General Governance" : category);
            draft.setRiskLevel(RiskLevel.fromText(node.path("riskLevel").asText("MEDIUM")));
            draft.setSourceExcerpt(node.path("sourceExcerpt").asText(""));
            if (node.path("sourcePage").canConvertToInt() && !node.path("sourcePage").isNull()) {
                int page = node.path("sourcePage").asInt();
                draft.setSourcePage(page > 0 ? page : null);
            }
            draft.setRationale(node.path("rationale").asText(""));
            draft.setRecommendation(node.path("recommendation").asText(""));
            draft.setInferred(node.path("inferred").asBoolean(false));
            drafts.add(draft);
            if (drafts.size() == MAX_FINDINGS) {
                break;
            }
        }
        return drafts;
    }

    static String stripFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
        trimmed = trimmed.replaceFirst("\\s*```$", "");
        return trimmed.trim();
    }

    void logFailure(Exception ex) {
        log.error("Groq obligation extraction failed: {}", ex.getMessage());
    }
}
