package com.MediHubAPI.dto.diagnosis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FetchDiagnosesResponse {
    private List<DiagnosisRowResponse> data;
    private String message;
}
