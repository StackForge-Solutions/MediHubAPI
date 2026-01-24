package com.MediHubAPI.dto.scheduling.session.plan;


import com.MediHubAPI.model.enums.SessionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record SessionScheduleIntervalDTO(
        Long id,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull SessionType sessionType,
        @NotNull @Min(1) Integer capacity
) {}
