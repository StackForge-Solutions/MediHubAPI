package com.MediHubAPI.dto.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyKeyResponse {

    private String idempotencyKey;
    private Instant expiresAt;
}
