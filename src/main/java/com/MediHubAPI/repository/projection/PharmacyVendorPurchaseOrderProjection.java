package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PharmacyVendorPurchaseOrderProjection {
    Long getPurchaseOrderId();
    String getPoNumber();
    LocalDate getOrderDate();
    String getStatus();
    Integer getItemCount();
    BigDecimal getNetAmount();
}
