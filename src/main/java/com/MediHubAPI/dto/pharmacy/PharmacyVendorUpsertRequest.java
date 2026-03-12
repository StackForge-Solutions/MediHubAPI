package com.MediHubAPI.dto.pharmacy;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyVendorUpsertRequest {

    @NotBlank(message = "vendorCode is required")
    @Size(max = 50, message = "vendorCode must not exceed 50 characters")
    private String vendorCode;

    @NotBlank(message = "vendorName is required")
    @Size(max = 255, message = "vendorName must not exceed 255 characters")
    private String vendorName;

    @Size(max = 255, message = "contactPerson must not exceed 255 characters")
    private String contactPerson;

    @Size(max = 20, message = "phone must not exceed 20 characters")
    private String phone;

    @Email(message = "email must be a valid email address")
    @Size(max = 255, message = "email must not exceed 255 characters")
    private String email;

    @Size(max = 30, message = "gstNo must not exceed 30 characters")
    private String gstNo;

    @Size(max = 50, message = "drugLicenseNo must not exceed 50 characters")
    private String drugLicenseNo;

    @Size(max = 255, message = "addressLine1 must not exceed 255 characters")
    private String addressLine1;

    @Size(max = 255, message = "addressLine2 must not exceed 255 characters")
    private String addressLine2;

    @Size(max = 100, message = "city must not exceed 100 characters")
    private String city;

    @Size(max = 100, message = "state must not exceed 100 characters")
    private String state;

    @Size(max = 20, message = "pincode must not exceed 20 characters")
    private String pincode;

    @Min(value = 0, message = "paymentTermsDays must be greater than or equal to 0")
    private Integer paymentTermsDays;

    private Boolean active;
}
