package com.MediHubAPI.dto.patient.register;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRegisterData {
    private Long id;
    private String hospitalId;
    private String fileNo;
    private String fullName;
    private String phone;

    @JsonProperty("isInternational")
    private boolean international;
}
