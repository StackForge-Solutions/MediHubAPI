package com.MediHubAPI.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabQueueItemDto {
    private String token;
    private Long patientId;
    private String patientName;
    private String ageLabel;
    private String phone;
    private String doctorName;
    private String referrerName;
    private String createdAtLabel;
    private String dateISO;
    private String status;
    private Boolean insurance;
    private Boolean referrer;
    private String notes;
    private String room;
}
