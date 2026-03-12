package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PurchaseOrderRowProjection {
    Long getPurchaseOrderId();
    String getPoNumber();
    Long getVendorId();
    String getVendorName();
    LocalDate getOrderDate();
    String getStatus();
    Long getItemCount();
    Integer getOrderedQty();
    Integer getReceivedQty();
    BigDecimal getNetAmount();
}
