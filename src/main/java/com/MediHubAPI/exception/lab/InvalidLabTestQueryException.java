package com.MediHubAPI.exception.lab;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class InvalidLabTestQueryException extends HospitalAPIException {
    public InvalidLabTestQueryException() {
        super(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "q must be at least 2 characters");
    }
}
