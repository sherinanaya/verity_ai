package com.verityai.analysis;

import com.verityai.assessment.AnalyzeResponse;
import com.verityai.assessment.AssessmentService;
import com.verityai.document.DocumentService;
import com.verityai.document.ExtractedDocument;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AnalyzeController {
    private final DocumentService documents;
    private final AnalysisService analysis;
    private final AssessmentService assessments;

    public AnalyzeController(DocumentService documents, AnalysisService analysis, AssessmentService assessments) {
        this.documents = documents;
        this.analysis = analysis;
        this.assessments = assessments;
    }

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AnalyzeResponse analyze(@RequestParam("file") MultipartFile file) {
        ExtractedDocument document = documents.extract(file);
        AnalysisResult result = analysis.analyze(document);
        return assessments.record(document, result);
    }
}
