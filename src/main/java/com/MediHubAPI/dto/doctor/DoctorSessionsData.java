package com.MediHubAPI.dto.doctor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorSessionsData {
    private Long doctorId;
    private Map<String, List<DoctorSessionDto>> sessionsByWeekday;
}
