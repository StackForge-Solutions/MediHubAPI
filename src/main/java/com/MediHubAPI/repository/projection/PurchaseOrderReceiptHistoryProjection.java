package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface PurchaseOrderReceiptHistoryProjection {
    Long getBatchId();
    Long getPurchaseOrderItemId();
    Long getMedicineId();
    String getMedicineName();
    String getBatchNo();
    LocalDate getExpiryDate();
    Integer getReceivedQty();
    BigDecimal getPurchasePrice();
    BigDecimal getMrp();
    BigDecimal getSellingPrice();
    LocalDateTime getReceivedAt();
}
