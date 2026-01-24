package com.MediHubAPI.exception.scheduling.session;

import com.MediHubAPI.scheduling.session.error.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class SessionSchedulingExceptionHandler {

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
                        Map.of()
                )
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        details.put("validationErrors", fieldErrors);

        return ResponseEntity.badRequest().body(
                new ErrorResponseDTO(
                        OffsetDateTime.now(),
                        400,
                        "Bad Request",
                        "VALIDATION_ERROR",
                        "Validation failed",
                        req.getRequestURI(),
                        details
                )
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex, HttpServletRequest req) {
        return ResponseEntity.internalServerError().body(
                new ErrorResponseDTO(
                        OffsetDateTime.now(),
                        500,
                        "Internal Server Error",
                        "INTERNAL_ERROR",
                        ex.getMessage(),
                        req.getRequestURI(),
                        Map.of()
                )
        );
    }
}
