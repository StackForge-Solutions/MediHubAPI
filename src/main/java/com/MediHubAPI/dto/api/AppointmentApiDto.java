package com.MediHubAPI.dto.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentApiDto {
    private String id;
    private Long doctorId;
    private String doctorName;
    private Long departmentId;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private String patientHospitalId;
    private String dateISO;
    private String timeHHmm;
    private String tokenNo;
    private String visitType;
    private String status;
    private String bookedBy;
    private String bookingSource;
    private Boolean isWalkin;
    private Boolean needsAttention;
    private String smsStatus;
    private String createdAtISO;
    private String notes;
}
