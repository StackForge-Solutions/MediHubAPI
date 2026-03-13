package com.MediHubAPI.dto.pharmacy;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAdjustmentResponseDto {
    private Long adjustmentId;
    private String adjustmentNo;
    private String status;
}
