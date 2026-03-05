package com.MediHubAPI.exception.diagnosis;

import org.springframework.http.HttpStatus;

public class DiagnosisInvalidInputException extends DiagnosisException {

    public DiagnosisInvalidInputException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }
}
