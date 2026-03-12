package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRowDto {
    private Long purchaseOrderId;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private LocalDate orderDate;
    private String status;
    private Long itemCount;
    private Integer orderedQty;
    private Integer receivedQty;
    private BigDecimal netAmount;
}
