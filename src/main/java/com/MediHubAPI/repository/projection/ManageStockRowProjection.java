package com.MediHubAPI.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ManageStockRowProjection {
    Long getMedicineId();
    String getMedicineCode();
    String getMedicineName();
    String getBrand();
    String getForm();
    Integer getAvailableQty();
    Integer getReservedQty();
    Integer getReorderLevel();
    BigDecimal getSellingPrice();
    BigDecimal getMrp();
    LocalDate getNearestExpiryDate();
    BigDecimal getStockValue();
}
