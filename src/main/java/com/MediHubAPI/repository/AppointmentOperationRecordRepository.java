package com.MediHubAPI.repository;

import com.MediHubAPI.model.AppointmentOperationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentOperationRecordRepository extends JpaRepository<AppointmentOperationRecord, Long> {
    Optional<AppointmentOperationRecord> findByIdempotencyKey(String idempotencyKey);
}
