package com.MediHubAPI.repository.projection;

public interface PharmacyVendorRowProjection {
    Long getVendorId();
    String getVendorCode();
    String getVendorName();
    String getContactPerson();
    String getPhone();
    String getEmail();
    String getGstNo();
    String getCity();
    Integer getPaymentTermsDays();
    Boolean getActive();
}
