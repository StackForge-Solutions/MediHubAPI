package com.MediHubAPI.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HospitalAPIException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    // ✅ existing 2-arg (keep if you already use)
    public HospitalAPIException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.code = null;
    }

    // ✅ NEW 3-arg constructor
    public HospitalAPIException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HospitalAPIException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
