package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int status;
    private String message;
    private String path;
    private Instant timestamp;
    private T data;

    public static <T> ApiResponse<T> success(T data, String path, String message) {
        return new ApiResponse<>(200, message, path, Instant.now(), data);
    }

    public static <T> ApiResponse<T> created(T data, String path, String message) {
        return new ApiResponse<>(201, message, path, Instant.now(), data);
    }

    public static <T> ApiResponse<T> ok(String message, String path) {
        return new ApiResponse<>(200, message, path, Instant.now(), null);
    }

    public static ApiResponse<Object> error(String requestURI, String idempotencyConflict, String message) {
        return new ApiResponse<>(500, message, requestURI, Instant.now(), null);
    }
}
