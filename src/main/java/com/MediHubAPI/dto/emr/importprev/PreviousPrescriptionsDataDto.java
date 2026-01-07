package com.MediHubAPI.dto.emr.importprev;

import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PreviousPrescriptionsDataDto {
    private List<PreviousPrescriptionItemDto> items;
    private Integer count;
    private Integer limit;

    private Long patientId;      // for patient endpoint
    private Long appointmentId;  // for appointment endpoint
}
