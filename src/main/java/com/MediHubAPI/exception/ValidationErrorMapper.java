package com.MediHubAPI.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import org.slf4j.MDC;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Collects validation errors into a normalized payload that can be reused by different handlers.
 */
@Component
public class ValidationErrorMapper {

    public ValidationProblem from(MethodArgumentNotValidException ex) {
        return fromBindingResult(ex.getBindingResult());
    }

    public ValidationProblem from(BindException ex) {
        return fromBindingResult(ex.getBindingResult());
    }

    private ValidationProblem fromBindingResult(BindingResult bindingResult) {
        Map<String, String> map = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            String field = normalizeFieldPath(fieldError.getField());
            String message = fieldError.getDefaultMessage();
            if (!map.containsKey(field)) {
                map.put(field, message);
            }
            errors.add(message);
        }

        bindingResult.getGlobalErrors().forEach(globalError -> {
            String field = globalError.getObjectName();
            String message = globalError.getDefaultMessage();
            if (!map.containsKey(field)) {
                map.put(field, message);
            }
            errors.add(message);
        });

        return new ValidationProblem(map, errors, "Validation failed");
    }

    public ValidationProblem from(ConstraintViolationException ex) {
        Map<String, String> map = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = formatPropertyPath(violation.getPropertyPath());
            if (field.isBlank()) {
                field = "_global";
            }
            String message = violation.getMessage();
            if (!map.containsKey(field)) {
                map.put(field, message);
            }
            errors.add(message);
        }

        return new ValidationProblem(map, errors, "Validation failed");
    }

    public ValidationProblem from(HttpMessageNotReadableException ex) {
        Map<String, String> map = new TreeMap<>();
        List<String> errors = new ArrayList<>();

        Throwable cause = ex.getMostSpecificCause();
        if (cause instanceof JsonMappingException mappingException) {
            String path = formatJsonPath(mappingException.getPath());
            String message = mappingException.getOriginalMessage();
            if (path.isBlank()) {
                path = "payload";
            }
            map.put(path, message);
            errors.add(message);
        } else if (cause instanceof JsonParseException) {
            String message = "Malformed JSON";
            map.put("payload", message);
            errors.add(message);
        } else if (cause instanceof MismatchedInputException mismatched) {
            String path = formatJsonPath(mismatched.getPath());
            String message = mismatched.getOriginalMessage();
            if (path.isBlank()) {
                path = "payload";
            }
            map.put(path, message);
            errors.add(message);
        } else {
            String message = cause != null ? cause.getMessage() : ex.getMessage();
            map.put("payload", message);
            errors.add(message);
        }

        return new ValidationProblem(map, errors, "Validation failed");
    }

    public String extractTraceId(HttpServletRequest request) {
        String mdcTrace = MDC.get("traceId");
        if (mdcTrace != null && !mdcTrace.isBlank()) {
            return mdcTrace;
        }

        List<String> headers = List.of("X-B3-TraceId", "X-Trace-Id", "traceId", "X-Correlation-Id");
        for (String header : headers) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizeFieldPath(String field) {
        return field == null ? "" : field;
    }

    private String formatJsonPath(List<JsonMappingException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : path) {
            if (ref.getFieldName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getFieldName());
            } else if (ref.getIndex() != -1) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    private String formatPropertyPath(jakarta.validation.Path propertyPath) {
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (jakarta.validation.Path.Node node : propertyPath) {
            ElementKind kind = node.getKind();
            if (kind == ElementKind.METHOD || kind == ElementKind.PARAMETER || kind == ElementKind.RETURN_VALUE || kind == ElementKind.CROSS_PARAMETER) {
                continue;
            }
            String name = node.getName();
            if (name == null || name.isBlank()) {
                continue;
            }
            if (started) {
                sb.append('.');
            }
            sb.append(name);
            if (node.getIndex() != null) {
                sb.append('[').append(node.getIndex()).append(']');
            } else if (node.getKey() != null) {
                sb.append('[').append(node.getKey()).append(']');
            }
            started = true;
        }
        return sb.toString();
    }

    public record ValidationProblem(Map<String, String> validationErrors, List<String> errors, String message) {
        public ValidationProblem {
            // Preserve insertion order but keep deterministic ordering for map
            validationErrors = validationErrors != null
                    ? new LinkedHashMap<>(validationErrors)
                    : new LinkedHashMap<>();
            errors = errors != null ? List.copyOf(errors) : List.of();
            message = message == null ? "Validation failed" : message;
        }
    }
}
