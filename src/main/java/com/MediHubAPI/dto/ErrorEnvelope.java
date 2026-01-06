package com.MediHubAPI.dto;

import lombok.*;

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
    }
}
