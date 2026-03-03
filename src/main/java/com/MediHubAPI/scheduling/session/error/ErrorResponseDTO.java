package com.MediHubAPI.scheduling.session.error;
import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponseDTO(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        Map<String, Object> details,
        Map<String, String> validationErrors,
        String errorCode,
        String traceId
) {}
