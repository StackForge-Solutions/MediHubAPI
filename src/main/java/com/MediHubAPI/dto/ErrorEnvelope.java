package com.MediHubAPI.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorEnvelope {
    private ErrorBody error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorBody {
        private String code;
        private String errorCode;
        private String message;
        private String field;   // optional single field pointer
        private List<Object> details;
        private Map<String, String> validationErrors;
        private String traceId;
        private Instant timestamp;
    }
}
