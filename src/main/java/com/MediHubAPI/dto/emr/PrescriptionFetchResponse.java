package com.MediHubAPI.dto.emr;

import com.MediHubAPI.dto.PrescribedTestDTO;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionFetchResponse {

    private String language;

    private Boolean followUpEnabled;
    private Integer followUpDuration;
    private String followUpUnit;
    private LocalDate followUpDate;

    private Boolean sendFollowUpEmail;

    private String adviceText;

    private List<MedicationResponse> medications;

    private List<PrescribedTestDTO> tests; // includes id
}
