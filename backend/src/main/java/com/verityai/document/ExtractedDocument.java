package com.verityai.document;

import java.util.List;

public record ExtractedDocument(String fileName, long sizeBytes, int totalPages, List<PageText> pages) {
}
