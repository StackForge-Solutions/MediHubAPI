// src/main/java/com/MediHubAPI/billing/model/InvoiceItem.java
package com.MediHubAPI.model.billing;

import com.MediHubAPI.dto.InvoiceDtos;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "invoice_items",
        indexes = {
                @Index(name = "idx_inv_item_invoice", columnList = "invoice_id")
        }
)
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "service_item_id")
    private Long serviceItemId; // link to DoctorServiceItem if used

    @Column(name = "sl_no", nullable = false)
    private Integer slNo;

    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit_price", precision = 14, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "tax_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal taxPercent; // e.g. 0 or 18.00

    @Column(name = "tax_amount", precision = 14, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "line_total", precision = 14, scale = 2, nullable = false)
    private BigDecimal lineTotal; // computed


    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 24)
    private InvoiceDtos.ItemType itemType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "code", length = 64)
    private String code;

    // ===== Lab result fields (used by Lab module) =====
    @Column(name = "sample_status", length = 24)
    private String sampleStatus; // PENDING/COMPLETED/NO_SHOW/RESERVED

    @Column(name = "result_value", length = 128)
    private String resultValue;

    @Column(name = "result_unit", length = 32)
    private String resultUnit;

    @Column(name = "reference_range", length = 96)
    private String referenceRange;

    @Column(name = "out_of_range")
    private Boolean outOfRange;

    @Column(name = "authorized")
    private Boolean authorized;

    @Column(name = "authorized_by", length = 128)
    private String authorizedBy;

    @Column(name = "authorized_at")
    private java.time.LocalDateTime authorizedAt;

    //  Optional: Auto-calculate lineTotal before persist
    @PrePersist
    @PreUpdate
    private void calculateLineTotal() {
        if (unitPrice != null && qty != null) {
            BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal afterDiscount = gross.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
            BigDecimal tax = taxAmount != null ? taxAmount : BigDecimal.ZERO;
            this.lineTotal = afterDiscount.add(tax);
        }
    }
}
