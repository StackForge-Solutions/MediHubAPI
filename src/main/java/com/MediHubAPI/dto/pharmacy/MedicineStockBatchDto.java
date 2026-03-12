package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineStockBatchDto {
    private Long batchId;
    private String batchNo;
    private Long vendorId;
    private String vendorName;
    private LocalDate expiryDate;
    private BigDecimal purchasePrice;
    private BigDecimal mrp;
    private BigDecimal sellingPrice;
    private Integer receivedQty;
    private Integer availableQty;
    private Boolean expired;
}
