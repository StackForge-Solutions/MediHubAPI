package com.MediHubAPI.exception.diagnosis;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public abstract class DiagnosisException extends HospitalAPIException {

    protected DiagnosisException(HttpStatus status, String code, String message) {
        super(status, code, message);
    }
}
