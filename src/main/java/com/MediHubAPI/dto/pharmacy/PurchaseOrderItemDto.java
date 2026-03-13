package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemDto {
    private Long itemId;
    private Long medicineId;
    private String medicineName;
    private Integer orderedQty;
    private Integer receivedQty;
    private Integer pendingQty;
    private BigDecimal purchasePrice;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private BigDecimal taxPercent;
    private BigDecimal discountPercent;
    private BigDecimal lineTotal;
}
