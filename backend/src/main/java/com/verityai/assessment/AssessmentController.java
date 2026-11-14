package com.verityai.assessment;

import com.verityai.obligation.ObligationResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {
    private final AssessmentService assessments;

    public AssessmentController(AssessmentService assessments) {
        this.assessments = assessments;
    }

    @GetMapping
    public List<AssessmentResponse> list() {
        return assessments.list();
    }

    @GetMapping("/{id}")
    public AssessmentDetailResponse get(@PathVariable Long id) {
        return assessments.get(id);
    }

    @GetMapping("/{id}/obligations")
    public List<ObligationResponse> obligations(@PathVariable Long id) {
        return assessments.obligationsFor(id);
    }
}
