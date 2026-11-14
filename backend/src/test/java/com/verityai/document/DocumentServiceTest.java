package com.verityai.document;

import com.verityai.common.DocumentRejectedException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentServiceTest {
    private final DocumentService documents = new DocumentService();

    @Test
    void extractsSelectableTextFromAPdf() throws Exception {
        byte[] pdf = pdf("Employees must report incidents.");
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", pdf);

        ExtractedDocument extracted = documents.extract(file);

        assertEquals("policy.pdf", extracted.fileName());
        assertEquals(1, extracted.totalPages());
        assertTrue(extracted.pages().get(0).text().contains("must report"));
    }

    @Test
    void rejectsNonPdfUploads() {
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        DocumentRejectedException error = assertThrows(DocumentRejectedException.class, () -> documents.extract(file));
        assertTrue(error.getMessage().contains("PDF"));
    }

    @Test
    void rejectsOversizedFiles() {
        MockMultipartFile file = new MockMultipartFile("file", "policy.pdf", "application/pdf", new byte[(int) DocumentService.MAX_BYTES + 1]);
        DocumentRejectedException error = assertThrows(DocumentRejectedException.class, () -> documents.extract(file));
        assertTrue(error.getMessage().contains("10 MB"));
    }

    private static byte[] pdf(String text) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(48, 720);
                stream.showText(text);
                stream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
