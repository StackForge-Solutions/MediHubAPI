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
@Table(name = "invoice_payments",
        indexes = {
                @Index(name = "idx_inv_payment_invoice", columnList = "invoice_id"),
                @Index(name = "idx_inv_payment_method", columnList = "method")
        })
public class InvoicePayment {

    public enum Method {CASH, CARD, UPI, INSURANCE, WALLET, OTHER, NETBANKING}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    @JsonIgnore
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 24)
    private Method method;

    @Column(name = "amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "txn_ref", length = 128)
    private String txnRef;

    // ✅ FIXED: use LocalDateTime instead of OffsetDateTime for MySQL compatibility
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
