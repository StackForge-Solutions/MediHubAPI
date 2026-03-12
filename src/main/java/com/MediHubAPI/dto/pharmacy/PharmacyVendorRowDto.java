package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyVendorRowDto {
    private Long vendorId;
    private String vendorCode;
    private String vendorName;
    private String contactPerson;
    private String phone;
    private String email;
    private String gstNo;
    private String city;
    private Integer paymentTermsDays;
    private Boolean active;
    private long outstandingPurchaseOrders;
    private LocalDate lastPurchaseDate;
}
