package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentItemDto {
    private Long medicineId;
    private String medicineName;
    private Long batchId;
    private String batchNo;
    private Integer qty;
    private BigDecimal unitCost;
    private BigDecimal lineValue;
    private String note;
}
