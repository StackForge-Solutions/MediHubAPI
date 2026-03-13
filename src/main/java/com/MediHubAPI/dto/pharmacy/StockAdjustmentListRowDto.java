package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentListRowDto {
    private Long adjustmentId;
    private String adjustmentNo;
    private LocalDate adjustmentDate;
    private String adjustmentType;
    private String reason;
    private Integer medicineCount;
    private Integer totalQtyImpact;
    private String createdBy;
    private String status;
}
