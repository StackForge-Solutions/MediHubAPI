package com.MediHubAPI.dto.emr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpAdmissionSaveResponse {
    private Long ipAdmissionId;
    private Long appointmentId;
    private String savedAt;
}
