package com.MediHubAPI.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSlotDto {
    private Long doctorId;
    private String dateISO;
    private String timeHHmm;
    private String status;
    private boolean isWalkin;
}
