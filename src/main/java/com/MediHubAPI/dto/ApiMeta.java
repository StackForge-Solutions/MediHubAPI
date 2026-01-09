package com.MediHubAPI.dto;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiMeta {
    private String traceId;
    private Instant timestamp;
}
