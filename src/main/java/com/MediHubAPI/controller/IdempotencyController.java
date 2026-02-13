package com.MediHubAPI.controller;

import com.MediHubAPI.dto.idempotency.IdempotencyKeyResponse;
import com.MediHubAPI.service.IdempotencyKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/idempotency")
@RequiredArgsConstructor
public class IdempotencyController {

    private final IdempotencyKeyService idempotencyKeyService;

    @PostMapping("/keys")
    public ResponseEntity<IdempotencyKeyResponse> createKey() {
        return ResponseEntity.ok(idempotencyKeyService.generate());
    }
}
