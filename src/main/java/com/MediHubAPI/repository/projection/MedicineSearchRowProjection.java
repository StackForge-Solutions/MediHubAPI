package com.MediHubAPI.repository.projection;

public interface MedicineSearchRowProjection {
    Long getId();
    String getForm();
    String getBrand();
    String getComposition();
    Integer getStockQty();
    Integer getInStock();
}
