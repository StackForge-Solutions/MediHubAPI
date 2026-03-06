package com.MediHubAPI.dto.emr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpAdmissionFetchResponse {
    private Long ipAdmissionId;
    private Long appointmentId;
    private LocalDate visitDate;
    private String admissionAdvised;
    private String remarks;
    private String admissionReason;
    private Integer tentativeStayDays;
    private String notes;
    private String savedAt;
}
