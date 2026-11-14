package com.verityai.analysis;

import com.verityai.document.PageText;

import java.util.List;
import java.util.Locale;

public final class EvidenceChecker {
    private EvidenceChecker() {
    }

    public static void reconcile(ObligationDraft draft, List<PageText> pages) {
        String excerpt = normalize(draft.getSourceExcerpt());
        if (excerpt.isBlank()) {
            draft.setSourceExcerpt("");
            draft.setSourcePage(null);
            return;
        }
        String probe = excerpt.length() > 180 ? excerpt.substring(0, 180) : excerpt;
        Integer matchedPage = null;
        for (PageText page : pages) {
            if (normalize(page.text()).contains(probe)) {
                matchedPage = page.page();
                break;
            }
        }
        if (matchedPage == null) {
            draft.setSourceExcerpt("");
            draft.setSourcePage(null);
            draft.setInferred(true);
            if (draft.getRationale() == null || draft.getRationale().isBlank()) {
                draft.setRationale("No exact supporting quote was found in the document. Treat this as an interpretation that requires human review.");
            }
            return;
        }
        draft.setSourcePage(matchedPage);
        draft.setSourceExcerpt(draft.getSourceExcerpt().trim());
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
