package com.MediHubAPI.dto.patient.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientSearchResponse {
    private int status;
    private List<PatientSearchResultDto> data;
    private String message;
}
