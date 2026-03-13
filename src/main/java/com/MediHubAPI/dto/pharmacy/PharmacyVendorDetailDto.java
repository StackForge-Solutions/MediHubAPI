package com.MediHubAPI.dto.pharmacy;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyVendorDetailDto {
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private String contactPerson;
    private String phone;
    private String email;
    private String gstNo;
    private String drugLicenseNo;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private Integer paymentTermsDays;
    private Boolean active;
    private PharmacyVendorStatsDto stats;
}
