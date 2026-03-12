package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PharmacyVendorStatsProjection {
    Long getTotalPurchaseOrders();
    Long getPendingPurchaseOrders();
    BigDecimal getTotalPurchaseValue();
    LocalDate getLastPurchaseDate();
}
