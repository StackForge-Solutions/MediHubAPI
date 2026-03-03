package com.MediHubAPI.service;

import com.MediHubAPI.config.IdempotencyProperties;
import com.MediHubAPI.dto.idempotency.IdempotencyKeyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyKeyService {

    private final IdempotencyProperties properties;

    public IdempotencyKeyResponse generate() {
        String key      = UUID.randomUUID().toString();
        Instant expires = Instant.now().plus(properties.getTtlHours(), ChronoUnit.HOURS);
        return IdempotencyKeyResponse.builder()
                                     .idempotencyKey(key)
                                     .expiresAt(expires)
                                     .build();
    }
}
