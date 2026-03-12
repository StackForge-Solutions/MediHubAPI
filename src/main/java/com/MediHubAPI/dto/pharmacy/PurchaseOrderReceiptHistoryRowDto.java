package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderReceiptHistoryRowDto {
    private Long batchId;
    private Long purchaseOrderItemId;
    private Long medicineId;
    private String medicineName;
    private String batchNo;
    private LocalDate expiryDate;
    private Integer receivedQty;
    private BigDecimal purchasePrice;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private Instant receivedAt;
}
