package com.MediHubAPI.exception.scheduling.session;

import com.MediHubAPI.exception.ValidationErrorMapper;
import com.MediHubAPI.scheduling.session.error.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.MediHubAPI.controller.scheduling.session")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SessionSchedulingExceptionHandler {

    private final ValidationErrorMapper validationErrorMapper;

    public SessionSchedulingExceptionHandler(ValidationErrorMapper validationErrorMapper) {
        this.validationErrorMapper = validationErrorMapper;
    }

    @ExceptionHandler(SchedulingException.class)
    public ResponseEntity<ErrorResponseDTO> handleScheduling(SchedulingException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.getStatus()).body(
                new ErrorResponseDTO(
                        OffsetDateTime.now(),
                        ex.getStatus().value(),
                        ex.getStatus().getReasonPhrase(),
                        ex.getCode(),
                        ex.getMessage(),
                        req.getRequestURI(),
                        Map.of(),
                        Map.of(),
                        ex.getCode(),
                        validationErrorMapper.extractTraceId(req)
                )
        );
    }
}
