package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSummaryDto {
    private long totalMedicines;
    private long lowStockCount;
    private long outOfStockCount;
    private long expiringSoonCount;
    private BigDecimal stockValue;
}
