package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyVendorPurchaseOrderRowDto {
    private Long purchaseOrderId;
    private String poNumber;
    private LocalDate orderDate;
    private String status;
    private Integer itemCount;
    private BigDecimal netAmount;
}
