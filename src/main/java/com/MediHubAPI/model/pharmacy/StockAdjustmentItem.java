package com.MediHubAPI.model.pharmacy;

import com.MediHubAPI.model.mdm.MdmMedicine;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "stock_adjustment_item",
        indexes = {
                @Index(name = "idx_stock_adjustment_item_adjustment", columnList = "stock_adjustment_id"),
                @Index(name = "idx_stock_adjustment_item_medicine", columnList = "medicine_id"),
                @Index(name = "idx_stock_adjustment_item_batch", columnList = "batch_id")
        }
)
public class StockAdjustmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_adjustment_id", nullable = false)
    private StockAdjustment stockAdjustment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private MdmMedicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private PharmacyStockBatch batch;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit_cost", precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "line_value", precision = 14, scale = 2)
    private BigDecimal lineValue;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
