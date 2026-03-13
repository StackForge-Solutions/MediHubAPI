package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyVendorStatsDto {
    private long totalPurchaseOrders;
    private long pendingPurchaseOrders;
    private BigDecimal totalPurchaseValue;
    private LocalDate lastPurchaseDate;
}
