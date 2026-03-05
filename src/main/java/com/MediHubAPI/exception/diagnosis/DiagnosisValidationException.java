package com.MediHubAPI.exception.diagnosis;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class DiagnosisValidationException extends DiagnosisException {

    private final Map<String, String> validationErrors;
    private final List<String> errors;
    private final String responseCode;

    public DiagnosisValidationException(
            String errorCode,
            String message,
            Map<String, String> validationErrors,
            List<String> errors
    ) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
        this.validationErrors = validationErrors;
        this.errors = errors;
        this.responseCode = "VALIDATION_ERROR";
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getResponseCode() {
        return responseCode;
    }
}
