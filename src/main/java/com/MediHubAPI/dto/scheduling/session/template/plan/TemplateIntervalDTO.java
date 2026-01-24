package com.MediHubAPI.dto.scheduling.session.template.plan;


import java.time.LocalTime;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.MediHubAPI.model.enums.SessionType;

public record TemplateIntervalDTO(
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull SessionType sessionType,
        @NotNull @Min(1) Integer capacity
) {}
