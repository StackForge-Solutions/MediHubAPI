package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDetailDto {
    private Long purchaseOrderId;
    private String poNumber;
    private Long vendorId;
    private String vendorName;
    private LocalDate orderDate;
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
    private String note;
    private List<PurchaseOrderItemDto> items;
}
