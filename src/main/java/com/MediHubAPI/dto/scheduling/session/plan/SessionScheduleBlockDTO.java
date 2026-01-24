package com.MediHubAPI.dto.scheduling.session.plan;

import com.MediHubAPI.model.enums.BlockType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record SessionScheduleBlockDTO(
        Long id,
        @NotNull BlockType blockType,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String reason
) {}
