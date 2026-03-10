package com.MediHubAPI.controller;

import java.util.List;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.MediHubAPI.dto.emr.EmrSaveCompleteResponse;
import com.MediHubAPI.exception.GlobalExceptionHandler;
import com.MediHubAPI.exception.ValidationErrorMapper;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.service.emr.EmrCompletionService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmrCompletionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private EmrCompletionService emrCompletionService;

    @BeforeEach
    void setUp() {
        EmrCompletionController controller = new EmrCompletionController(emrCompletionService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(new ValidationErrorMapper());
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    @DisplayName("POST /api/emr/appointments/{appointmentId}/complete-consultation updates queue to PHARMACY")
    void saveAndCompleteReturnsSuccessResponse() throws Exception {
        when(emrCompletionService.completeConsultationAndRouteInvoiceToPharmacy(7L)).thenReturn(
                new EmrSaveCompleteResponse(7L, 11L, 21L, "PHARMACY", "DRAFT", "COMPLETED")
        );

        mockMvc.perform(post("/api/emr/appointments/7/complete-consultation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("EMR saved and completed successfully"))
                .andExpect(jsonPath("$.path").value("/api/emr/appointments/7/complete-consultation"))
                .andExpect(jsonPath("$.data.appointmentId").value(7))
                .andExpect(jsonPath("$.data.visitSummaryId").value(11))
                .andExpect(jsonPath("$.data.invoiceId").value(21))
                .andExpect(jsonPath("$.data.invoiceQueue").value("PHARMACY"))
                .andExpect(jsonPath("$.data.invoiceStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.appointmentStatus").value("COMPLETED"));

        verify(emrCompletionService).completeConsultationAndRouteInvoiceToPharmacy(7L);
    }

    @Test
    @DisplayName("POST /api/emr/appointments/{appointmentId}/complete-consultation rejects non-positive appointmentId")
    void saveAndCompleteRejectsInvalidAppointmentId() throws Exception {
        when(emrCompletionService.completeConsultationAndRouteInvoiceToPharmacy(0L)).thenThrow(
                new ValidationException(
                        "Validation failed",
                        List.of(new ValidationException.ValidationErrorDetail(
                                "appointmentId",
                                "appointmentId must be a positive integer"
                        ))
                )
        );

        mockMvc.perform(post("/api/emr/appointments/0/complete-consultation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/emr/appointments/0/complete-consultation"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(
                        jsonPath("$.validationErrors.appointmentId").value("appointmentId must be a positive integer"))
                .andExpect(jsonPath("$.errors[0]").value("appointmentId must be a positive integer"));

        verify(emrCompletionService).completeConsultationAndRouteInvoiceToPharmacy(0L);
    }

    @Test
    @DisplayName("POST /api/emr/appointments/{appointmentId}/complete-consultation returns validation payload from service")
    void saveAndCompleteReturnsServiceValidationErrors() throws Exception {
        when(emrCompletionService.completeConsultationAndRouteInvoiceToPharmacy(9L)).thenThrow(
                new ValidationException(
                        "Validation failed",
                        List.of(new ValidationException.ValidationErrorDetail(
                                "invoice",
                                "consultation invoice not found for this appointment"
                        ))
                )
        );

        mockMvc.perform(post("/api/emr/appointments/9/complete-consultation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/emr/appointments/9/complete-consultation"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.invoice").value(
                        "consultation invoice not found for this appointment"))
                .andExpect(jsonPath("$.errors[0]").value("consultation invoice not found for this appointment"));

        verify(emrCompletionService).completeConsultationAndRouteInvoiceToPharmacy(9L);
    }
}
