package com.MediHubAPI.dto.emr.importprev;

import lombok.*;

import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PreviousPrescriptionItemDto {
    private Long appointmentId;
    private Long prescriptionId;
    private LocalDate visitDate;

    private Long doctorId;
    private String doctorName;

    private String summary; // optional (if you have VisitSummary text, else null/"")

    private PrescriptionPayloadDto payload;
}
