package com.verityai.obligation;

import com.verityai.common.ApiExceptionHandler;
import com.verityai.common.NotFoundException;
import com.verityai.common.ReviewStatus;
import com.verityai.common.RiskLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObligationControllerTest {
    private ObligationService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ObligationService.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);
        mvc = MockMvcBuilders.standaloneSetup(new ObligationController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    void updatesReviewStatus() throws Exception {
        when(service.updateStatus(4L, ReviewStatus.CONFIRMED)).thenReturn(sample(ReviewStatus.CONFIRMED));

        mvc.perform(put("/api/obligations/4/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.requirement").value("Staff must report incidents."));
    }

    @Test
    void rejectsAnUnknownStatus() throws Exception {
        mvc.perform(put("/api/obligations/4/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void returnsNotFoundWhenTheObligationIsMissing() throws Exception {
        when(service.get(9L)).thenThrow(new NotFoundException("Obligation not found."));

        mvc.perform(get("/api/obligations/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Obligation not found."));
    }

    @Test
    void deletesAnObligation() throws Exception {
        mvc.perform(delete("/api/obligations/4"))
                .andExpect(status().isNoContent());
        verify(service).delete(4L);
    }

    @Test
    void filtersByRisk() throws Exception {
        when(service.list(null, RiskLevel.HIGH, null, null)).thenReturn(List.of(sample(ReviewStatus.NEEDS_REVIEW)));

        mvc.perform(get("/api/obligations").param("risk", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].riskLevel").value("HIGH"));
    }

    private static ObligationResponse sample(ReviewStatus status) {
        return new ObligationResponse(
                4L,
                1L,
                "policy.pdf",
                "Staff must report incidents.",
                "Incident Management",
                RiskLevel.HIGH,
                status,
                2,
                "Staff must report incidents.",
                "Human review is required.",
                "Confirm the incident owner.",
                false,
                null,
                null);
    }
}
