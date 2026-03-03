package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appointment_operation_record", uniqueConstraints = {
        @UniqueConstraint(columnNames = "idempotency_key")
})
public class AppointmentOperationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, length = 80, unique = true)
    private String idempotencyKey;

    @Column(name = "operation", nullable = false, length = 20)
    private String operation;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "operation_date", nullable = false)
    private LocalDate date;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Lob
    @Column(name = "response_payload", nullable = false, columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
