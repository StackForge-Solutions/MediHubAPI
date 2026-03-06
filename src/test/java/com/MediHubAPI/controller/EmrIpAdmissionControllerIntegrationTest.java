package com.MediHubAPI.controller;

import java.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.MediHubAPI.controller.emr.EmrIpAdmissionController;
import com.MediHubAPI.dto.emr.IpAdmissionFetchResponse;
import com.MediHubAPI.dto.emr.IpAdmissionSaveRequest;
import com.MediHubAPI.dto.emr.IpAdmissionSaveResponse;
import com.MediHubAPI.exception.GlobalExceptionHandler;
import com.MediHubAPI.exception.ValidationErrorMapper;
import com.MediHubAPI.service.emr.IpAdmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EmrIpAdmissionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private IpAdmissionService ipAdmissionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        EmrIpAdmissionController controller = new EmrIpAdmissionController(ipAdmissionService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(new ValidationErrorMapper());
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /api/emr/appointments/{id}/ip-admission returns prefilled payload")
    void getIpAdmissionReturnsPrefill() throws Exception {
        IpAdmissionFetchResponse resp = IpAdmissionFetchResponse.builder()
                .ipAdmissionId(10L)
                .appointmentId(5L)
                .visitDate(LocalDate.parse("2026-03-06"))
                .admissionAdvised("yes")
                .remarks("Needs close monitoring")
                .admissionReason("Surgical")
                .tentativeStayDays(2)
                .notes("Admit under Dr. X")
                .savedAt("2026-03-06T10:00:00Z")
                .build();

        when(ipAdmissionService.getIpAdmission(5L)).thenReturn(resp);

        mockMvc.perform(get("/api/emr/appointments/{appointmentId}/ip-admission", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(5))
                .andExpect(jsonPath("$.ipAdmissionId").value(10))
                .andExpect(jsonPath("$.visitDate").value("2026-03-06"))
                .andExpect(jsonPath("$.admissionAdvised").value("yes"))
                .andExpect(jsonPath("$.admissionReason").value("Surgical"))
                .andExpect(jsonPath("$.tentativeStayDays").value(2))
                .andExpect(jsonPath("$.notes").value("Admit under Dr. X"))
                .andExpect(jsonPath("$.savedAt").value("2026-03-06T10:00:00Z"));

        verify(ipAdmissionService).getIpAdmission(5L);
    }

    @Test
    @DisplayName("POST /api/emr/appointments/{id}/ip-admission upserts and returns payload")
    void saveIpAdmissionUpserts() throws Exception {
        IpAdmissionSaveResponse resp = IpAdmissionSaveResponse.builder()
                .ipAdmissionId(11L)
                .appointmentId(5L)
                .savedAt("2026-03-06T11:00:00Z")
                .build();
        when(ipAdmissionService.saveIpAdmission(eq(5L), any(IpAdmissionSaveRequest.class))).thenReturn(resp);

        String requestBody = """
                {
                  "visitDate": "2026-03-06",
                  "admissionAdvised": "yes",
                  "remarks": "Updated remarks",
                  "admissionReason": "Surgical",
                  "tentativeStayDays": 3,
                  "notes": "Updated notes"
                }
                """;

        mockMvc.perform(post("/api/emr/appointments/{appointmentId}/ip-admission", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ipAdmissionId").value(11))
                .andExpect(jsonPath("$.appointmentId").value(5))
                .andExpect(jsonPath("$.savedAt").value("2026-03-06T11:00:00Z"));

        verify(ipAdmissionService).saveIpAdmission(eq(5L), any(IpAdmissionSaveRequest.class));
    }

    @Test
    @DisplayName("POST /api/emr/appointments/{id}/ip-admission without visitDate returns validation error")
    void saveIpAdmissionWithoutVisitDateReturnsValidationError() throws Exception {
        String requestBody = """
                {
                  "admissionAdvised": "yes",
                  "remarks": "Updated remarks",
                  "admissionReason": "Surgical",
                  "tentativeStayDays": 3,
                  "notes": "Updated notes"
                }
                """;

        mockMvc.perform(post("/api/emr/appointments/{appointmentId}/ip-admission", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.visitDate").value("visitDate is required"));

        verifyNoInteractions(ipAdmissionService);
    }

    @Test
    @DisplayName("POST /api/emr/appointments/{id}/ip-admission with admissionAdvised=na succeeds without reason")
    void saveIpAdmissionWithNa() throws Exception {
        IpAdmissionSaveResponse resp = IpAdmissionSaveResponse.builder()
                .ipAdmissionId(12L)
                .appointmentId(6L)
                .savedAt("2026-03-06T12:00:00Z")
                .build();
        when(ipAdmissionService.saveIpAdmission(eq(6L), any(IpAdmissionSaveRequest.class))).thenReturn(resp);

        String requestBody = """
                {
                  "visitDate": "2026-03-07",
                  "admissionAdvised": "na",
                  "remarks": "Undecided",
                  "notes": "Pending decision"
                }
                """;

        mockMvc.perform(post("/api/emr/appointments/{appointmentId}/ip-admission", 6L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ipAdmissionId").value(12))
                .andExpect(jsonPath("$.appointmentId").value(6))
                .andExpect(jsonPath("$.savedAt").value("2026-03-06T12:00:00Z"));

        verify(ipAdmissionService).saveIpAdmission(eq(6L), any(IpAdmissionSaveRequest.class));
    }

    @Test
    @DisplayName("POST /api/emr/appointments/{id}/ip-admission with invalid admissionAdvised returns error")
    void saveIpAdmissionWithInvalidAdmissionAdvised() throws Exception {
        String requestBody = """
                {
                  "visitDate": "2026-03-08",
                  "admissionAdvised": "maybe",
                  "remarks": "TBD",
                  "notes": "Check later"
                }
                """;

        mockMvc.perform(post("/api/emr/appointments/{appointmentId}/ip-admission", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.admissionAdvised").value(
                        "admissionAdvised must be one of: yes, no, na"));

        verifyNoInteractions(ipAdmissionService);
    }
}
