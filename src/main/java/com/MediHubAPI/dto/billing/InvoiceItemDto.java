package com.MediHubAPI.dto.billing;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemDto {
    private Long id;
    private String type;         // LAB / CONSULT / PHARM etc
    private String serviceCode;
    private String serviceName;
    private Double unitPrice;
    private Integer quantity;
    private Double discount;
    private Boolean taxable;
    private Double lineTotal;
}
