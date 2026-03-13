package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface MedicineBatchProjection {
    Long getBatchId();
    String getBatchNo();
    Long getVendorId();
    String getVendorName();
    LocalDate getExpiryDate();
    BigDecimal getPurchasePrice();
    BigDecimal getMrp();
    BigDecimal getSellingPrice();
    Integer getReceivedQty();
    Integer getAvailableQty();
    Boolean getExpired();
}
