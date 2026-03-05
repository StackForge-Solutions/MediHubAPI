package com.MediHubAPI.controller;

import com.MediHubAPI.dto.diagnosis.DiagnosisRowResponse;
import com.MediHubAPI.exception.GlobalExceptionHandler;
import com.MediHubAPI.exception.ValidationErrorMapper;
import com.MediHubAPI.service.DiagnosisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiagnosisControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private DiagnosisService diagnosisService;

    @BeforeEach
    void setUp() {
        DiagnosisController controller = new DiagnosisController(diagnosisService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(new ValidationErrorMapper());
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("POST /api/diagnoses returns created response for valid payload")
    void createDiagnosisReturnsCreatedResponse() throws Exception {
        DiagnosisRowResponse row = new DiagnosisRowResponse(
                "Type 2 Diabetes Mellitus",
                "3 yrs, 2 mths",
                Instant.parse("2023-01-01T00:00:00Z"),
                true,
                false,
                "Follow-up needed"
        );
        when(diagnosisService.createDiagnosis(eq(7L), any())).thenReturn(row);

        String requestBody = """
                {
                  "appointmentId": 7,
                  "source": "primary",
                  "name": "Type 2 Diabetes Mellitus",
                  "years": 3,
                  "months": 2,
                  "days": 0,
                  "sinceYear": 2023,
                  "chronic": true,
                  "primary": false,
                  "comments": "Follow-up needed"
                }
                """;

        mockMvc.perform(post("/api/diagnoses")
                        .param("appointmentId", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Diagnosis created successfully"))
                .andExpect(jsonPath("$.data.name").value("Type 2 Diabetes Mellitus"))
                .andExpect(jsonPath("$.data.sinceLabel").value("3 yrs, 2 mths"))
                .andExpect(jsonPath("$.data.sinceDate").value("2023-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.chronic").value(true))
                .andExpect(jsonPath("$.data.primary").value(false))
                .andExpect(jsonPath("$.data.comments").value("Follow-up needed"));

        verify(diagnosisService).createDiagnosis(eq(7L), any());
    }

    @Test
    @DisplayName("GET /api/diagnoses returns list payload for valid appointmentId")
    void fetchDiagnosesReturnsListPayload() throws Exception {
        List<DiagnosisRowResponse> rows = List.of(
                new DiagnosisRowResponse(
                        "Type 2 Diabetes Mellitus",
                        "3 yrs, 2 mths",
                        Instant.parse("2023-01-01T00:00:00Z"),
                        true,
                        false,
                        "Follow-up needed"
                ),
                new DiagnosisRowResponse(
                        "Hypothyroidism",
                        null,
                        Instant.parse("2024-06-01T00:00:00Z"),
                        true,
                        true,
                        ""
                )
        );
        when(diagnosisService.fetchDiagnoses(7L)).thenReturn(rows);

        mockMvc.perform(get("/api/diagnoses").param("appointmentId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Diagnoses fetched successfully"))
                .andExpect(jsonPath("$.data[0].name").value("Type 2 Diabetes Mellitus"))
                .andExpect(jsonPath("$.data[0].sinceLabel").value("3 yrs, 2 mths"))
                .andExpect(jsonPath("$.data[0].sinceDate").value("2023-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[0].chronic").value(true))
                .andExpect(jsonPath("$.data[0].primary").value(false))
                .andExpect(jsonPath("$.data[0].comments").value("Follow-up needed"))
                .andExpect(jsonPath("$.data[1].name").value("Hypothyroidism"))
                .andExpect(jsonPath("$.data[1].sinceDate").value("2024-06-01T00:00:00Z"))
                .andExpect(jsonPath("$.data[1].chronic").value(true))
                .andExpect(jsonPath("$.data[1].primary").value(true))
                .andExpect(jsonPath("$.data[1].comments").value(""))
                .andExpect(jsonPath("$.data[1].sinceLabel").doesNotExist());

        verify(diagnosisService).fetchDiagnoses(7L);
    }

    @Test
    @DisplayName("GET /api/diagnoses without appointmentId returns DIAGNOSIS_002 validation payload")
    void fetchDiagnosesWithoutAppointmentIdReturnsDiagnosisValidationError() throws Exception {
        mockMvc.perform(get("/api/diagnoses"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("appointmentId is required"))
                .andExpect(jsonPath("$.path").value("/api/diagnoses"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorCode").value("DIAGNOSIS_002"))
                .andExpect(jsonPath("$.validationErrors.appointmentId").value("appointmentId is required"))
                .andExpect(jsonPath("$.errors[0]").value("Invalid appointment id"));

        verifyNoInteractions(diagnosisService);
    }

    @Test
    @DisplayName("GET /api/diagnoses with non-positive appointmentId returns DIAGNOSIS_002")
    void fetchDiagnosesWithInvalidAppointmentIdReturnsDiagnosisValidationError() throws Exception {
        mockMvc.perform(get("/api/diagnoses").param("appointmentId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("appointmentId must be a positive integer"))
                .andExpect(jsonPath("$.path").value("/api/diagnoses"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorCode").value("DIAGNOSIS_002"))
                .andExpect(jsonPath("$.validationErrors.appointmentId").value("appointmentId must be a positive integer"))
                .andExpect(jsonPath("$.errors[0]").value("Invalid appointment id"));

        verifyNoInteractions(diagnosisService);
    }

    @Test
    @DisplayName("POST /api/diagnoses with mismatched appointmentId returns INVALID_INPUT")
    void createDiagnosisWithMismatchedAppointmentIdsReturnsInvalidInput() throws Exception {
        String requestBody = """
                {
                  "appointmentId": 8,
                  "source": "primary",
                  "name": "Type 2 Diabetes Mellitus",
                  "years": 3,
                  "months": 2,
                  "days": 0,
                  "sinceYear": 2023,
                  "chronic": true,
                  "primary": false,
                  "comments": "Follow-up needed"
                }
                """;

        mockMvc.perform(post("/api/diagnoses")
                        .param("appointmentId", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("appointmentId in query and request body must match"))
                .andExpect(jsonPath("$.path").value("/api/diagnoses"))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));

        verifyNoInteractions(diagnosisService);
    }

    @Test
    @DisplayName("PUT /api/diagnoses returns updated payload for valid request")
    void updateDiagnosisReturnsSuccessPayload() throws Exception {
        DiagnosisRowResponse row = new DiagnosisRowResponse(
                "Type 2 Diabetes Mellitus with nephropathy",
                "3 yrs, 2 mths",
                Instant.parse("2023-01-01T00:00:00Z"),
                true,
                true,
                "Updated notes"
        );
        when(diagnosisService.updateDiagnosis(eq(123L), any())).thenReturn(row);

        String requestBody = """
                {
                  "appointmentId": 123,
                  "currentName": "Type 2 Diabetes Mellitus",
                  "source": "primary",
                  "name": "Type 2 Diabetes Mellitus with nephropathy",
                  "years": 3,
                  "months": 2,
                  "days": 0,
                  "sinceYear": null,
                  "chronic": true,
                  "primary": true,
                  "comments": "Updated notes"
                }
                """;

        mockMvc.perform(put("/api/diagnoses")
                        .param("appointmentId", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Diagnosis updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Type 2 Diabetes Mellitus with nephropathy"))
                .andExpect(jsonPath("$.data.sinceLabel").value("3 yrs, 2 mths"))
                .andExpect(jsonPath("$.data.sinceDate").value("2023-01-01T00:00:00Z"))
                .andExpect(jsonPath("$.data.chronic").value(true))
                .andExpect(jsonPath("$.data.primary").value(true))
                .andExpect(jsonPath("$.data.comments").value("Updated notes"));

        verify(diagnosisService).updateDiagnosis(eq(123L), any());
    }

    @Test
    @DisplayName("PUT /api/diagnoses with missing currentName returns DIAGNOSIS_002 payload")
    void updateDiagnosisMissingCurrentNameReturnsDiagnosisValidationPayload() throws Exception {
        String requestBody = """
                {
                  "appointmentId": 123,
                  "currentName": "",
                  "source": "primary",
                  "name": "Type 2 Diabetes Mellitus with nephropathy",
                  "years": 3,
                  "months": 2,
                  "days": 0,
                  "sinceYear": null,
                  "chronic": true,
                  "primary": true,
                  "comments": "Updated notes"
                }
                """;

        mockMvc.perform(put("/api/diagnoses")
                        .param("appointmentId", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/diagnoses"))
                .andExpect(jsonPath("$.code").value("DIAGNOSIS_002"))
                .andExpect(jsonPath("$.errorCode").value("DIAGNOSIS_002"))
                .andExpect(jsonPath("$.validationErrors.currentName").value("currentName is required"))
                .andExpect(jsonPath("$.errors[0]").value("Diagnosis not found"));

        verifyNoInteractions(diagnosisService);
    }
}
