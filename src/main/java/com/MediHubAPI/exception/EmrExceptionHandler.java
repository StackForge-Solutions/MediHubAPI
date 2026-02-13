package com.MediHubAPI.exception;

import com.MediHubAPI.dto.ErrorEnvelope;
import com.MediHubAPI.exception.billing.InvoiceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice(basePackages = "com.MediHubAPI.controller.emr")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmrExceptionHandler {

    private final ValidationErrorMapper validationErrorMapper;

    public EmrExceptionHandler(ValidationErrorMapper validationErrorMapper) {
        this.validationErrorMapper = validationErrorMapper;
    }

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

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorEnvelope> handleBodyValidation(Exception ex, HttpServletRequest request) {
        ValidationErrorMapper.ValidationProblem problem =
                ex instanceof MethodArgumentNotValidException manve
                        ? validationErrorMapper.from(manve)
                        : validationErrorMapper.from((BindException) ex);

        return buildValidationResponse(problem, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorEnvelope> handleConstraintValidation(ConstraintViolationException ex, HttpServletRequest request) {
        ValidationErrorMapper.ValidationProblem problem = validationErrorMapper.from(ex);
        return buildValidationResponse(problem, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorEnvelope> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ValidationErrorMapper.ValidationProblem problem = validationErrorMapper.from(ex);
        return buildValidationResponse(problem, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorEnvelope> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ValidationErrorMapper.ValidationProblem problem = new ValidationErrorMapper.ValidationProblem(
                Map.of("_global", ex.getMessage()),
                List.of(ex.getMessage()),
                "Validation failed"
        );
        return buildValidationResponse(problem, request);
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

    private ResponseEntity<ErrorEnvelope> buildValidationResponse(ValidationErrorMapper.ValidationProblem problem, HttpServletRequest request) {
        Map<String, String> validationErrors = new LinkedHashMap<>(problem.validationErrors());
        String traceId = validationErrorMapper.extractTraceId(request);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorEnvelope.builder()
                        .error(ErrorEnvelope.ErrorBody.builder()
                                .code("VALIDATION_ERROR")
                                .errorCode("VALIDATION_ERROR")
                                .message(problem.message())
                                .validationErrors(validationErrors)
                                .traceId(traceId)
                                .timestamp(Instant.now())
                                .build())
                        .build()
        );
    }
}
