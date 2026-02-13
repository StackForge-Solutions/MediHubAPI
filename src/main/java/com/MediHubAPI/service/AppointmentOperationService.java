package com.MediHubAPI.service;

import com.MediHubAPI.config.IdempotencyProperties;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.exception.visits.IdempotencyConflictException;
import com.MediHubAPI.model.AppointmentOperationRecord;
import com.MediHubAPI.model.enums.AppointmentOperationType;
import com.MediHubAPI.repository.AppointmentOperationRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class AppointmentOperationService {

    private final AppointmentOperationRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final IdempotencyProperties idempotencyProperties;

    @Transactional
    public <T> T execute(String idempotencyKey,
                         AppointmentOperationMeta<T> meta,
                         Supplier<T> action) {
        AppointmentOperationRecord existing = repository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (isExpired(existing)) {
                repository.delete(existing);
            } else {
                if (!existing.getOperation().equals(meta.operation().name())) {
                    throw new IdempotencyConflictException("Idempotency key already used for a different operation");
                }
                if (!existing.getRequestHash().equals(meta.requestHash())) {
                    throw new IdempotencyConflictException("Idempotency key already used for another request payload");
                }
                return deserialize(existing.getResponsePayload(), meta.responseType());
            }
        }

        T result = action.get();
        String payload = serialize(result);

        AppointmentOperationRecord record = AppointmentOperationRecord.builder()
                .idempotencyKey(idempotencyKey)
                .operation(meta.operation().name())
                .doctorId(meta.doctorId())
                .date(meta.date())
                .requestHash(meta.requestHash())
                .responsePayload(payload)
                .createdAt(Instant.now())
                .build();
        repository.save(record);
        return result;
    }

    private <T> T deserialize(String payload, Class<T> type) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (JsonProcessingException ex) {
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_READ_FAILURE",
                    "Failed to read cached operation result", ex);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_WRITE_FAILURE",
                    "Failed to cache operation result", ex);
        }
    }

    private boolean isExpired(AppointmentOperationRecord record) {
        long ttlHours = idempotencyProperties.getTtlHours();
        if (ttlHours <= 0) {
            return false;
        }
        Instant expiresAt = record.getCreatedAt().plusSeconds(ttlHours * 3600);
        return Instant.now().isAfter(expiresAt);
    }

    public record AppointmentOperationMeta<T>(AppointmentOperationType operation,
                                             Long doctorId,
                                             LocalDate date,
                                             String requestHash,
                                             Class<T> responseType) {
    }
}
