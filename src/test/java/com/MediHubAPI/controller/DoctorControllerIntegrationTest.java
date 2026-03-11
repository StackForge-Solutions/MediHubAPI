package com.MediHubAPI.controller;

import com.MediHubAPI.exception.GlobalExceptionHandler;
import com.MediHubAPI.exception.ValidationErrorMapper;
import com.MediHubAPI.service.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DoctorControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private DoctorService doctorService;

    @BeforeEach
    void setUp() {
        DoctorController controller = new DoctorController(doctorService);
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(new ValidationErrorMapper());
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    @DisplayName("GET /doctors returns global validation payload for invalid pagination")
    void getAllDoctorsRejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/doctors")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/doctors"))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors.page").value("page must be greater than or equal to 0"))
                .andExpect(jsonPath("$.validationErrors.size").value("size must be greater than or equal to 1"));

        verifyNoInteractions(doctorService);
    }
}
