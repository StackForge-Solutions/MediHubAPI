package com.MediHubAPI.dto.pharmacy;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderReceiveResponseDto {
    private Long purchaseOrderId;
    private String status;
    private Integer receivedItemCount;
    private Integer receivedQty;
}
