package com.MediHubAPI.exception;

import com.MediHubAPI.dto.ErrorEnvelope;
import com.MediHubAPI.exception.billing.InvoiceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class EmrExceptionHandler {

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorEnvelope> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ErrorEnvelope.builder()
                        .error(ErrorEnvelope.ErrorBody.builder()
                                .code(ex.getCode())
                                .message(ex.getMessage())
                                .build())
                        .build()
        );
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorEnvelope> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorEnvelope.builder()
                        .error(ErrorEnvelope.ErrorBody.builder()
                                .code("VALIDATION_ERROR")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .build())
                        .build()
        );
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorEnvelope> handleCustomValidation(ValidationException ex) {
        List<Object> details = null;
        if (ex.getDetails() != null) {
            details = ex.getDetails().stream()
                    .map(d -> java.util.Map.of("field", d.getField(), "message", d.getMessage()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorEnvelope.builder()
                        .error(ErrorEnvelope.ErrorBody.builder()
                                .code("VALIDATION_ERROR")
                                .message(ex.getMessage())
                                .field(ex.getDetails() != null && !ex.getDetails().isEmpty() ? ex.getDetails().get(0).getField() : null)
                                .details(details)
                                .timestamp(Instant.now())
                                .build())
                        .build()
        );
    }

    @ExceptionHandler(InvoiceNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> handleInvoiceNotFound(InvoiceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorEnvelope.builder()
                        .error(ErrorEnvelope.ErrorBody.builder()
                                .code("NOT_FOUND")
                                .message(ex.getMessage())
                                .timestamp(Instant.now())
                                .build())
                        .build()
        );
    }
}
