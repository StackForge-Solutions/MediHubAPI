package com.MediHubAPI.repository.projection;

public interface LabTestMasterRowProjection {
    String getCode();
    String getName();
    Double getAmount();
    Integer getTaxable();     // 1/0 from SQL
    Integer getTatHours();
    String getSampleType();
    Integer getActive();      // 1/0 from SQL
    String getUpdatedAtISO();
}
