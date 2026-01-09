package com.MediHubAPI.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

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
        private String message;
        private List<Object> details;
        private String traceId;
        private Instant timestamp;
    }
}
