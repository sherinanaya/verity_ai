package com.verityai.analysis;

import com.verityai.common.RiskLevel;
import com.verityai.document.ExtractedDocument;
import com.verityai.document.PageText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedObligationExtractor {
    private static final int MAX_FINDINGS = 12;

    public List<ObligationDraft> extract(ExtractedDocument document) {
        List<ObligationDraft> drafts = new ArrayList<>();
        for (PageText page : document.pages()) {
            for (String sentence : sentences(page.text())) {
                String low = sentence.toLowerCase(Locale.ROOT);
                boolean mandatory = isMandatory(low);
                boolean ambiguous = isAmbiguous(low);
                if (!mandatory && !ambiguous) {
                    continue;
                }
                ObligationDraft draft = new ObligationDraft();
                draft.setRequirement(sentence);
                draft.setCategory(category(low));
                draft.setRiskLevel(risk(low, mandatory));
                draft.setSourcePage(page.page());
                draft.setSourceExcerpt(sentence);
                draft.setInferred(!mandatory);
                draft.setRationale(mandatory
                        ? "This sentence uses mandatory or assigned-duty language in the uploaded policy. It is a candidate finding for human review, not a legal determination."
                        : "This language is advisory or ambiguous. It may be a gap in enforceability and requires human review before any compliance conclusion.");
                draft.setRecommendation(recommendation(draft.getCategory(), mandatory));
                drafts.add(draft);
            }
        }
        drafts.sort(Comparator
                .comparing(ObligationDraft::isInferred)
                .thenComparing(draft -> rank(draft.getRiskLevel())));
        if (drafts.size() > MAX_FINDINGS) {
            return new ArrayList<>(drafts.subList(0, MAX_FINDINGS));
        }
        return drafts;
    }

    static List<String> sentences(String text) {
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        String[] parts = normalized.split("(?<=[.;])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String sentence = part.trim();
            if (sentence.length() >= 35) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    private static boolean isMandatory(String low) {
        return low.contains("must ") || low.startsWith("must ")
                || low.contains("shall ") || low.startsWith("shall ")
                || low.contains("required to") || low.contains("is responsible for")
                || low.contains("are responsible for");
    }

    private static boolean isAmbiguous(String low) {
        return low.contains("should ") || low.startsWith("should ")
                || low.contains("as appropriate") || low.contains("where practicable")
                || low.contains("timely manner") || low.contains("as soon as practicable");
    }

    private static RiskLevel risk(String low, boolean mandatory) {
        if (low.contains("within 24") || low.contains("personal data breach") || low.contains("without undue delay")) {
            return RiskLevel.CRITICAL;
        }
        if (low.contains("breach") || low.contains("unauthorized") || low.contains("incident")
                || low.contains("penalty") || low.contains("immediately")) {
            return RiskLevel.HIGH;
        }
        if (!mandatory) {
            return RiskLevel.LOW;
        }
        return RiskLevel.MEDIUM;
    }

    private static int rank(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> 0;
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 3;
        };
    }

    static String category(String low) {
        if (low.contains("retain") || low.contains("retention") || low.contains("destroy") || low.contains("disposal")) {
            return "Retention";
        }
        if (low.contains("report") || low.contains("notify") || low.contains("notification") || low.contains("deadline")) {
            return "Reporting";
        }
        if (low.contains("password") || low.contains("authentication") || low.contains("access")) {
            return "Access Control";
        }
        if (low.contains("personal data") || low.contains("privacy") || low.contains("data subject")) {
            return "Data Protection";
        }
        if (low.contains("incident") || low.contains("breach")) {
            return "Incident Management";
        }
        if (low.contains("encrypt") || low.contains("vulnerability") || low.contains("patch") || low.contains("malware")) {
            return "Security Controls";
        }
        if (low.contains("audit") || low.contains("monitor") || low.contains("log")) {
            return "Audit & Monitoring";
        }
        return "General Governance";
    }

    private static String recommendation(String category, boolean mandatory) {
        String next = switch (category) {
            case "Retention" -> "Confirm the retention period, the system of record, and the disposal owner.";
            case "Reporting" -> "Confirm who must be notified, the deadline, and where the evidence of notification is kept.";
            case "Access Control" -> "Confirm the control owner, the access review frequency, and the evidence of enforcement.";
            case "Data Protection" -> "Confirm the data covered, the lawful handling steps, and the evidence a reviewer can inspect.";
            case "Incident Management" -> "Confirm the incident owner, the escalation path, and the clock that starts the deadline.";
            case "Security Controls" -> "Confirm the control is implemented, who operates it, and which evidence proves it.";
            case "Audit & Monitoring" -> "Confirm the log source, the review cadence, and where exceptions are recorded.";
            default -> "Confirm an owner, the evidence of implementation, and a review frequency.";
        };
        if (!mandatory) {
            return "Rewrite the statement as an enforceable control with an owner and a deadline. " + next;
        }
        return next;
    }
}
