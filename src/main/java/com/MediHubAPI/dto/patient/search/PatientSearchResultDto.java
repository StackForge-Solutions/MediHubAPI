package com.MediHubAPI.dto.patient.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSearchResultDto {
    private Long id;
    private String hospitalId;
    private String fileNo;
    private String fullName;
    private String phone;
    private String fatherName;
    private String motherName;
    private String dobISO;
    private Boolean isInternational;
}
