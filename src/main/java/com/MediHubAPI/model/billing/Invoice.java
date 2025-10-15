// src/main/java/com/MediHubAPI/billing/model/Invoice.java
package com.MediHubAPI.model.billing;

import com.MediHubAPI.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices",
        indexes = {
                @Index(name = "idx_invoice_billnumber", columnList = "bill_number", unique = true),
                @Index(name = "idx_invoice_doctor", columnList = "doctor_id"),
                @Index(name = "idx_invoice_patient", columnList = "patient_id"),
                @Index(name = "idx_invoice_status", columnList = "status")
        })
public class Invoice {

    public enum Status {DRAFT, ISSUED, PAID, PARTIALLY_PAID, VOID}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient; // You also have Patient entity; use whichever you prefer consistently.

    @Column(name = "appointment_id")
    private Long appointmentId; // soft link to Appointment

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    @Column(name = "bill_number", length = 32, unique = true)
    private String billNumber; // assigned on finalize

    // clinic meta (denormalized for fast prints)
    @Column(name = "clinic_id", length = 64)
    private String clinicId;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency = "INR";

    // display extras
    @Column(name = "token_no")
    private Integer token;

    @Column(name = "queue", length = 128)
    private String queue;

    @Column(name = "room", length = 64)
    private String room;

    @Column(name = "notes", length = 512)
    private String notes;

    // totals (server-computed)
    @Column(name = "sub_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal subTotal;

    @Column(name = "discount_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal discountTotal;

    @Column(name = "tax_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal taxTotal;

    @Column(name = "round_off", precision = 14, scale = 2, nullable = false)
    private BigDecimal roundOff;

    @Column(name = "grand_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal grandTotal;

    @Column(name = "paid_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal paidAmount;

    @Column(name = "balance_due", precision = 14, scale = 2, nullable = false)
    private BigDecimal balanceDue;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 128)
    private String createdBy;

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoicePayment> payments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = Status.DRAFT;
        if (currency == null) currency = "INR";
        if (subTotal == null) subTotal = BigDecimal.ZERO;
        if (discountTotal == null) discountTotal = BigDecimal.ZERO;
        if (taxTotal == null) taxTotal = BigDecimal.ZERO;
        if (roundOff == null) roundOff = BigDecimal.ZERO;
        if (grandTotal == null) grandTotal = BigDecimal.ZERO;
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;
        if (balanceDue == null) balanceDue = BigDecimal.ZERO;
    }
}
