package com.MediHubAPI.dto.scheduling.session.plan;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.util.List;

public record SessionScheduleDayPlanDTO(
        @NotNull DayOfWeek dayOfWeek,
        boolean dayOff,

        @Valid
        @Size(max = 50)
        List<SessionScheduleIntervalDTO> intervals,

        @Valid
        @Size(max = 50)
        List<SessionScheduleBlockDTO> blocks
) {}
