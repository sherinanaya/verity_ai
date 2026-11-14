package com.verityai.obligation;

import com.verityai.common.ReviewStatus;
import com.verityai.common.RiskLevel;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/obligations")
public class ObligationController {
    private final ObligationService obligations;

    public ObligationController(ObligationService obligations) {
        this.obligations = obligations;
    }

    @GetMapping
    public List<ObligationResponse> list(
            @RequestParam(required = false) Long assessmentId,
            @RequestParam(required = false) RiskLevel risk,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) String q) {
        return obligations.list(assessmentId, risk, status, q);
    }

    @GetMapping("/{id}")
    public ObligationResponse get(@PathVariable Long id) {
        return obligations.get(id);
    }

    @PutMapping("/{id}/status")
    public ObligationResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return obligations.updateStatus(id, request.getStatus());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        obligations.delete(id);
    }
}
