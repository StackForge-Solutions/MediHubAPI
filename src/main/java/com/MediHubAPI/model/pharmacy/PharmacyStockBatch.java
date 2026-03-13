package com.MediHubAPI.model.pharmacy;

import com.MediHubAPI.model.mdm.MdmMedicine;
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
        name = "pharmacy_stock_batch",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pharmacy_stock_batch_medicine_batch_expiry",
                        columnNames = {"medicine_id", "batch_no", "expiry_date"})
        },
        indexes = {
                @Index(name = "idx_pharmacy_stock_batch_medicine", columnList = "medicine_id"),
                @Index(name = "idx_pharmacy_stock_batch_expiry", columnList = "expiry_date"),
                @Index(name = "idx_pharmacy_stock_batch_vendor", columnList = "vendor_id")
        }
)
public class PharmacyStockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private MdmMedicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private PharmacyVendor vendor;

    @Column(name = "batch_no", nullable = false, length = 100)
    private String batchNo;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "purchase_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "mrp", nullable = false, precision = 14, scale = 2)
    private BigDecimal mrp;

    @Column(name = "selling_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "received_qty", nullable = false)
    private Integer receivedQty = 0;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty = 0;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "purchase_order_item_id")
    private Long purchaseOrderItemId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
