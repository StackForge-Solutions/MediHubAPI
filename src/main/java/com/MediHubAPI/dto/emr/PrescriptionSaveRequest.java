package com.MediHubAPI.dto.emr;

import com.MediHubAPI.dto.PrescribedTestDTO;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionSaveRequest {

    private String language;

    private Boolean followUpEnabled;
    private Integer followUpDuration;
    private String followUpUnit;     // "Days"
    private LocalDate followUpDate;  // can be null
    private Boolean sendFollowUpEmail;

    private String adviceText;

    private List<MedicationRequest> medications;
    private List<PrescribedTestDTO> tests; // reuse your existing DTO
}
