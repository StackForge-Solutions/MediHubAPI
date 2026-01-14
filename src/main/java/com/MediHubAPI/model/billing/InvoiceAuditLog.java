package com.MediHubAPI.model.billing;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "invoice_audit_log",
        indexes = {
                @Index(name = "idx_inv_audit_invoice", columnList = "invoice_id"),
                @Index(name = "idx_inv_audit_appt", columnList = "appointment_id"),
                @Index(name = "idx_inv_audit_created", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceAuditLog {

    public enum Action {
        DRAFT_UPSERT_CREATED,
        DRAFT_UPSERT_UPDATED,
        DRAFT_UPSERT_DENIED,
        PAYMENT_ADDED,
        FINALIZED,
        VOIDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", length = 64, nullable = false)
    private Action action;

    @Column(name = "actor", length = 128)
    private String actor;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
