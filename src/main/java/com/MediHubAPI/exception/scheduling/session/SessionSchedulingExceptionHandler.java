package com.MediHubAPI.exception.scheduling.session;

import com.MediHubAPI.exception.ValidationErrorMapper;
import com.MediHubAPI.scheduling.session.error.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        ValidationErrorMapper.ValidationProblem problem = validationErrorMapper.from(ex);
        return buildValidationResponse(problem, req);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponseDTO> handleBindValidation(BindException ex, HttpServletRequest req) {
        ValidationErrorMapper.ValidationProblem problem = validationErrorMapper.from(ex);
        return buildValidationResponse(problem, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintValidation(ConstraintViolationException ex, HttpServletRequest req) {
        ValidationErrorMapper.ValidationProblem problem = validationErrorMapper.from(ex);
        return buildValidationResponse(problem, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ValidationErrorMapper.ValidationProblem problem = validationErrorMapper.from(ex);
        return buildValidationResponse(problem, req);
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
                        Map.of(),
                        Map.of(),
                        "INTERNAL_ERROR",
                        validationErrorMapper.extractTraceId(req)
                )
        );
    }

    private ResponseEntity<ErrorResponseDTO> buildValidationResponse(ValidationErrorMapper.ValidationProblem problem, HttpServletRequest req) {
        Map<String, Object> details = new HashMap<>();
        details.put("validationErrors", problem.validationErrors());

        return ResponseEntity.badRequest().body(
                new ErrorResponseDTO(
                        OffsetDateTime.now(),
                        400,
                        "Bad Request",
                        "VALIDATION_ERROR",
                        "Validation failed",
                        req.getRequestURI(),
                        details,
                        problem.validationErrors(),
                        "VALIDATION_ERROR",
                        validationErrorMapper.extractTraceId(req)
                )
        );
    }
}
