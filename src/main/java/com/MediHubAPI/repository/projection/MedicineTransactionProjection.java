package com.MediHubAPI.repository.projection;

import java.time.LocalDateTime;

public interface MedicineTransactionProjection {
    Long getTransactionId();
    LocalDateTime getTransactionTime();
    String getTransactionType();
    String getBatchNo();
    Integer getQtyIn();
    Integer getQtyOut();
    Integer getBalanceAfter();
    String getReferenceType();
    Long getReferenceId();
    String getReferenceNo();
    String getNote();
}
