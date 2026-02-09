package com.MediHubAPI.exception;

import lombok.Getter;

import java.util.List;

/**
 * Service-layer validation error with field-level details.
 */
@Getter
public class ValidationException extends RuntimeException {

    private final List<ValidationErrorDetail> details;

    public ValidationException(String message, List<ValidationErrorDetail> details) {
        super(message);
        this.details = details;
    }

    @Getter
    public static class ValidationErrorDetail {
        private final String field;
        private final String message;

        public ValidationErrorDetail(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
