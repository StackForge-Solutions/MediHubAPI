package com.MediHubAPI.dto.pharmacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyQueueItemDto {
    private int rowId;
    private String tokenNo;
    private String patientId;
    private String patientName;
    private String doctorName;
    private String createdAt;   // ISO string
    private boolean hasInsurance;
    private boolean hasReferrer;
    private String status;
}
