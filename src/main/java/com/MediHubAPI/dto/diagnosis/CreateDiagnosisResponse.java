package com.MediHubAPI.dto.diagnosis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDiagnosisResponse {
    private DiagnosisRowResponse data;
    private String message;
}
