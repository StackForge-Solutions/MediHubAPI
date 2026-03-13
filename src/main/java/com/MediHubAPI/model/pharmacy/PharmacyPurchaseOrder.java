package com.MediHubAPI.model.pharmacy;

import com.MediHubAPI.enums.pharmacy.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "pharmacy_purchase_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pharmacy_po_number", columnNames = "po_number")
        },
        indexes = {
                @Index(name = "idx_pharmacy_po_vendor", columnList = "vendor_id"),
                @Index(name = "idx_pharmacy_po_status", columnList = "status"),
                @Index(name = "idx_pharmacy_po_order_date", columnList = "order_date"),
                @Index(name = "idx_pharmacy_po_number", columnList = "po_number")
        }
)
public class PharmacyPurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private PharmacyVendor vendor;

    @Column(name = "po_number", nullable = false, length = 100)
    private String poNumber;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private PurchaseOrderStatus status;

    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 14, scale = 2)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", precision = 14, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "net_amount", precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
