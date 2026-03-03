package com.MediHubAPI.exception;

import com.MediHubAPI.dto.ErrorResponse;
import com.MediHubAPI.dto.appointments.AppointmentBlockRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Positive;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.Set;

class ValidationErrorIntegrationTest {

    private final ValidationErrorMapper mapper = new ValidationErrorMapper();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(mapper);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Body validation yields validationErrors map and legacy fields")
    void bodyValidationReturnsValidationErrorsMap() {
        AppointmentBlockRequest target = new AppointmentBlockRequest();
        target.setDoctorId(-1L);
        BindException bindException = new BindException(target, "appointmentBlockRequest");
        bindException.addError(new FieldError("appointmentBlockRequest", "doctorId", "doctorId must be positive"));
        bindException.addError(new FieldError("appointmentBlockRequest", "startHHmm", "startHHmm is required"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/appointments/blocks");

        ResponseEntity<ErrorResponse> response = handler.handleBindExceptions(bindException, request);

        Assertions.assertThat(response.getStatusCodeValue()).isEqualTo(400);
        ErrorResponse body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.getValidationErrors())
                .containsEntry("doctorId", "doctorId must be positive")
                .containsEntry("startHHmm", "startHHmm is required");
        Assertions.assertThat(body.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        Assertions.assertThat(body.getPath()).isEqualTo("/api/appointments/blocks");
    }

    @Test
    @DisplayName("Request parameter validation returns field path map")
    void requestParamValidationReturnsMap() {
        ConstraintViolationException violationException = new ConstraintViolationException(makeDoctorIdViolation(-1L));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/slots");

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolations(violationException, request);

        Assertions.assertThat(response.getStatusCodeValue()).isEqualTo(400);
        ErrorResponse body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.getValidationErrors())
                .containsEntry("doctorId", "must be greater than 0");
        Assertions.assertThat(body.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        Assertions.assertThat(body.getPath()).isEqualTo("/api/slots");
    }

    @Test
    @DisplayName("Bad JSON/type mismatch surfaces in validationErrors map")
    void jsonParseErrorReturnsValidationErrors() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "Invalid JSON",
                new com.fasterxml.jackson.core.JsonParseException(null, "Malformed JSON"),
                null
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/appointments/blocks");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidFormat(ex, request);

        Assertions.assertThat(response.getStatusCodeValue()).isEqualTo(400);
        ErrorResponse body = response.getBody();
        Assertions.assertThat(body).isNotNull();
        Assertions.assertThat(body.getValidationErrors())
                .containsEntry("payload", "Malformed JSON");
        Assertions.assertThat(body.getErrorCode()).isEqualTo("VALIDATION_ERROR");
        Assertions.assertThat(body.getPath()).isEqualTo("/api/appointments/blocks");
    }

    private Set<ConstraintViolation<DoctorIdWrapper>> makeDoctorIdViolation(Long doctorId) {
        DoctorIdWrapper wrapper = new DoctorIdWrapper(doctorId);
        return validator.validate(wrapper);
    }

    private record DoctorIdWrapper(@Positive Long doctorId) {}
}
