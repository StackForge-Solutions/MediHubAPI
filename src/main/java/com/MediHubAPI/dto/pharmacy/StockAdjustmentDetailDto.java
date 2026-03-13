package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentDetailDto {
    private Long adjustmentId;
    private String adjustmentNo;
    private LocalDate adjustmentDate;
    private String adjustmentType;
    private String reason;
    private String note;
    private String createdBy;
    private String status;
    private List<StockAdjustmentItemDto> items;
}
