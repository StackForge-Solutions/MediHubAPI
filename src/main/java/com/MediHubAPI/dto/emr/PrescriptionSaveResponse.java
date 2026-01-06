package com.MediHubAPI.dto.emr;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionSaveResponse {
    private Long prescriptionId;
    private Long appointmentId;
    private String savedAt; // ISO string
}
