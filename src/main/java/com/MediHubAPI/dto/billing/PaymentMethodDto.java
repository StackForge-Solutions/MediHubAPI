package com.MediHubAPI.dto.billing;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodDto {
    private String mode;   // CASH / CARD / UPI / etc
    private Double amount;
    private String refNo;
}
