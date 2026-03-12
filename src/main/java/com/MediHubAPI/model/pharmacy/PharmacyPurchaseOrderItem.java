package com.MediHubAPI.model.pharmacy;

import com.MediHubAPI.model.mdm.MdmMedicine;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "pharmacy_purchase_order_item",
        indexes = {
                @Index(name = "idx_pharmacy_po_item_po", columnList = "purchase_order_id"),
                @Index(name = "idx_pharmacy_po_item_medicine", columnList = "medicine_id")
        }
)
public class PharmacyPurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PharmacyPurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private MdmMedicine medicine;

    @Column(name = "ordered_qty", nullable = false)
    private Integer orderedQty = 0;

    @Column(name = "received_qty", nullable = false)
    private Integer receivedQty = 0;

    @Column(name = "purchase_price", precision = 14, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "mrp", precision = 14, scale = 2)
    private BigDecimal mrp;

    @Column(name = "selling_price", precision = 14, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "tax_percent", precision = 8, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "discount_percent", precision = 8, scale = 2)
    private BigDecimal discountPercent;

    @Column(name = "line_total", precision = 14, scale = 2)
    private BigDecimal lineTotal;
}
