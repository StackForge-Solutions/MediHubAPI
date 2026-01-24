package com.MediHubAPI.model.scheduling.template.plan;


import java.time.LocalTime;
import jakarta.validation.constraints.NotNull;
import com.MediHubAPI.model.enums.BlockType;

public record TemplateBlockDTO(
        @NotNull BlockType blockType,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String reason
) {}
