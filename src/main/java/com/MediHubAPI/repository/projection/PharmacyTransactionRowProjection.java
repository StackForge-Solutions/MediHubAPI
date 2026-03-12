package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PharmacyTransactionRowProjection {
    Long getTransactionId();
    LocalDateTime getTransactionTime();
    Long getMedicineId();
    String getMedicineName();
    Long getBatchId();
    String getBatchNo();
    Long getVendorId();
    String getVendorName();
    String getTransactionType();
    Integer getQtyIn();
    Integer getQtyOut();
    Integer getBalanceAfter();
    BigDecimal getUnitCost();
    BigDecimal getUnitPrice();
    String getReferenceType();
    Long getReferenceId();
    String getReferenceNo();
    String getCreatedBy();
    String getNote();
}
