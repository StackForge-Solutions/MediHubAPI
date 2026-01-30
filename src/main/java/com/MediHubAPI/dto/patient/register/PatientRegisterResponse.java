package com.MediHubAPI.dto.patient.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRegisterResponse {
    private int status;
    private PatientRegisterData data;
    private String message;
}
