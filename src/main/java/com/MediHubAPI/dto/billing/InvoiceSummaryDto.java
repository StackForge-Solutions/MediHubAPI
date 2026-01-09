package com.MediHubAPI.dto.billing;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceSummaryDto {
    private Double subTotal;
    private Double discountTotal;
    private Double taxTotal;
    private Double netPayable;
    private Double amountPaid;
    private Double balance;
}
