package com.MediHubAPI.model.pharmacy;

import com.MediHubAPI.enums.pharmacy.StockAdjustmentReason;
import com.MediHubAPI.enums.pharmacy.StockAdjustmentStatus;
import com.MediHubAPI.enums.pharmacy.StockAdjustmentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "stock_adjustment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_adjustment_no", columnNames = "adjustment_no")
        },
        indexes = {
                @Index(name = "idx_stock_adjustment_no", columnList = "adjustment_no"),
                @Index(name = "idx_stock_adjustment_date", columnList = "adjustment_date"),
                @Index(name = "idx_stock_adjustment_type", columnList = "adjustment_type"),
                @Index(name = "idx_stock_adjustment_reason", columnList = "reason"),
                @Index(name = "idx_stock_adjustment_status", columnList = "status")
        }
)
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "adjustment_no", nullable = false, length = 100)
    private String adjustmentNo;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDate adjustmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 30)
    private StockAdjustmentType adjustmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 50)
    private StockAdjustmentReason reason;

    @Column(name = "note", length = 500)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StockAdjustmentStatus status;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "stockAdjustment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockAdjustmentItem> items = new ArrayList<>();

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
