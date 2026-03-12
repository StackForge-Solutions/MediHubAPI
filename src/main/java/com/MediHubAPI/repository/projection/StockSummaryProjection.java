package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;

public interface StockSummaryProjection {
    Long getTotalMedicines();
    Long getLowStockCount();
    Long getOutOfStockCount();
    Long getExpiringSoonCount();
    BigDecimal getStockValue();
}
