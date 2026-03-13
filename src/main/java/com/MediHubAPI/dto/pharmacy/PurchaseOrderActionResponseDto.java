package com.MediHubAPI.dto.pharmacy;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderActionResponseDto {
    private Long purchaseOrderId;
    private String poNumber;
    private String status;
}
