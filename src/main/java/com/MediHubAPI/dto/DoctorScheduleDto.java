package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleDto {
    private Long id;
    private String name;
    private String specialization;
    private Long departmentId;
    private String avatarUrl;
    private List<HourlySlotGroupDto> timeSlots;
}