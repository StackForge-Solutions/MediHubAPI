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
        name = "pharmacy_stock_transaction",
        indexes = {
                @Index(name = "idx_pharmacy_stock_txn_medicine", columnList = "medicine_id"),
                @Index(name = "idx_pharmacy_stock_txn_batch", columnList = "batch_id"),
                @Index(name = "idx_pharmacy_stock_txn_type", columnList = "transaction_type"),
                @Index(name = "idx_pharmacy_stock_txn_time", columnList = "transaction_time"),
                @Index(name = "idx_pharmacy_stock_txn_ref", columnList = "reference_type, reference_id")
        }
)
public class PharmacyStockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private MdmMedicine medicine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private PharmacyStockBatch batch;

    @Column(name = "transaction_type", nullable = false, length = 50)
    private String transactionType;

    @Column(name = "qty_in", nullable = false)
    private Integer qtyIn = 0;

    @Column(name = "qty_out", nullable = false)
    private Integer qtyOut = 0;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter = 0;

    @Column(name = "unit_cost", precision = 14, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "reference_no", length = 100)
    private String referenceNo;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;
}
