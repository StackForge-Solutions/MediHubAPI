package com.MediHubAPI.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;

    private String code;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private Instant timestamp;

    private Map<String, String> validationErrors;  // Changed from 'errors' to 'validationErrors'

    public ErrorResponse(int status, String error, String message, String path, Instant timestamp) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.timestamp = timestamp;
    }


}