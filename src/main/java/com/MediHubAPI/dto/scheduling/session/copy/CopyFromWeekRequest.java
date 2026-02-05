package com.MediHubAPI.dto.scheduling.session.copy;

import com.MediHubAPI.model.enums.CopyStrategy;
import com.MediHubAPI.model.enums.ScheduleMode;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CopyFromWeekRequest(
        @NotNull ScheduleMode mode,
        Long doctorId,
        Long templateId,
        @NotNull LocalDate fromWeekStartISO,
        @NotNull LocalDate toWeekStartISO,
        @NotNull CopyStrategy strategy,
        @NotNull Boolean includeBlocks,
        @NotNull Boolean includeDayOffFlags
) {}
