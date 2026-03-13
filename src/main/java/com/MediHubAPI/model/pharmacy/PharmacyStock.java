package com.MediHubAPI.model.pharmacy;

import com.MediHubAPI.model.mdm.MdmMedicine;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "pharmacy_stock",
        uniqueConstraints = @UniqueConstraint(name = "uk_stock_medicine", columnNames = "medicine_id"),
        indexes = {
                @Index(name = "idx_pharmacy_stock_medicine", columnList = "medicine_id")
        }
)
public class PharmacyStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One row per medicine in stock table (simple current-stock model)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private MdmMedicine medicine;

    @Column(name = "available_qty", nullable = false)
    private Integer availableQty = 0;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty = 0;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
