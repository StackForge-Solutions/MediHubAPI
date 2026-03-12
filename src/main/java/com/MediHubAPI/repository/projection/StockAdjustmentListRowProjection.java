package com.MediHubAPI.repository.projection;

import java.time.LocalDate;

public interface StockAdjustmentListRowProjection {
    Long getAdjustmentId();
    String getAdjustmentNo();
    LocalDate getAdjustmentDate();
    String getAdjustmentType();
    String getReason();
    Long getMedicineCount();
    Long getTotalQtyImpact();
    String getCreatedBy();
    String getStatus();
}
