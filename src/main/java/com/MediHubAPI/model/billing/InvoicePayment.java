package com.MediHubAPI.model.billing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "invoice_payments",
        uniqueConstraints = {
                // Retry safety
                @UniqueConstraint(name = "uk_invoice_payment_idem", columnNames = {"invoice_id", "idempotency_key"}),

                // CASH/OTHER server receipt uniqueness per invoice
                @UniqueConstraint(name = "uk_invoice_payment_receipt", columnNames = {"invoice_id", "receipt_no"}),

                // Gateway hard dedupe (global)
                @UniqueConstraint(name = "uk_invoice_payment_method_txn", columnNames = {"method", "txn_ref"})
        },
        indexes = {
                @Index(name = "idx_inv_payment_invoice", columnList = "invoice_id"),
                @Index(name = "idx_inv_payment_method", columnList = "method"),
                @Index(name = "idx_inv_payment_idem", columnList = "invoice_id,idempotency_key"),
                @Index(name = "idx_inv_payment_receipt", columnList = "invoice_id,receipt_no"),
                @Index(name = "idx_inv_payment_txn", columnList = "method,txn_ref")
        }
)
public class InvoicePayment {

    public enum Method {CASH, CARD, UPI, INSURANCE, WALLET, OTHER, NETBANKING}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @Column(name = "idempotency_key", nullable = false, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 24)
    private Method method;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    // Gateway ref for UPI/CARD/NETBANKING
    @Column(name = "txn_ref", length = 128)
    private String txnRef;

    // Server generated for CASH/OTHER
    @Column(name = "receipt_no", length = 64)
    private String receiptNo;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "received_by", length = 128)
    private String receivedBy;

    @Column(name = "notes", length = 512)
    private String notes;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "card_type", length = 32)
    private String cardType;

    @PrePersist
    void onCreate() {
        if (receivedAt == null) receivedAt = LocalDateTime.now();
    }
}
