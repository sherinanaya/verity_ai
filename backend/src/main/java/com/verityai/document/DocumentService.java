package com.verityai.document;

import com.verityai.common.DocumentRejectedException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class DocumentService {
    static final long MAX_BYTES = 10L * 1024 * 1024;

    public ExtractedDocument extract(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentRejectedException("Please select a PDF file.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new DocumentRejectedException("File must be smaller than 10 MB.");
        }
        String name = baseName(file.getOriginalFilename());
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        boolean pdfName = name.toLowerCase(Locale.ROOT).endsWith(".pdf");
        boolean pdfType = contentType.equalsIgnoreCase("application/pdf")
                || contentType.equalsIgnoreCase("application/x-pdf");
        if (!pdfName && !pdfType) {
            throw new DocumentRejectedException("Only PDF files are supported.");
        }
        if (!pdfName) {
            name = name + ".pdf";
        }
        byte[] bytes = read(file);
        if (bytes.length == 0) {
            throw new DocumentRejectedException("Please select a PDF file.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new DocumentRejectedException("File must be smaller than 10 MB.");
        }
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            if (pdf.isEncrypted()) {
                throw new DocumentRejectedException("Encrypted PDFs are not supported.");
            }
            int totalPages = pdf.getNumberOfPages();
            if (totalPages == 0) {
                throw new DocumentRejectedException("The PDF has no pages.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            List<PageText> pages = new ArrayList<>();
            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = stripper.getText(pdf);
                if (text != null && !text.isBlank()) {
                    pages.add(new PageText(page, text.trim()));
                }
            }
            if (pages.isEmpty()) {
                throw new DocumentRejectedException(
                        "No selectable text found. Scanned PDFs need OCR; try a text-based PDF.");
            }
            return new ExtractedDocument(name, bytes.length, totalPages, pages);
        } catch (DocumentRejectedException ex) {
            throw ex;
        } catch (InvalidPasswordException ex) {
            throw new DocumentRejectedException("Encrypted PDFs are not supported.");
        } catch (IOException ex) {
            throw new DocumentRejectedException("The file is not a readable PDF.");
        }
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new DocumentRejectedException("The file could not be read.");
        }
    }

    private static String baseName(String original) {
        String name = Optional.ofNullable(original).orElse("").trim();
        if (name.isBlank()) {
            return "policy.pdf";
        }
        String fileName = Paths.get(name).getFileName().toString();
        return fileName.isBlank() ? "policy.pdf" : fileName;
    }
}
