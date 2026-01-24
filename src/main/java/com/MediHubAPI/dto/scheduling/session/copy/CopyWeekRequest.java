package com.MediHubAPI.dto.scheduling.session.copy;

import com.MediHubAPI.model.enums.MergeStrategy;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CopyWeekRequest(
        @NotNull Long doctorId,

        @NotNull LocalDate sourceWeekStartISO,
        @NotNull LocalDate targetWeekStartISO,

        @NotNull MergeStrategy mergeStrategy
) {}
