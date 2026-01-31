package com.MediHubAPI.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSessionDto {
    private String label;
    private String startHHmm;
    private String endHHmm;
}
