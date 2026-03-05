package com.MediHubAPI.exception.diagnosis;

import org.springframework.http.HttpStatus;

public class DuplicateDiagnosisException extends DiagnosisException {

    public DuplicateDiagnosisException() {
        super(HttpStatus.BAD_REQUEST, "DIAGNOSIS_001", "Diagnosis already exists");
    }
}
