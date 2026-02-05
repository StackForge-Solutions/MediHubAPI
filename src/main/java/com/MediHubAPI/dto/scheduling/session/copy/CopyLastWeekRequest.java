package com.MediHubAPI.dto.scheduling.session.copy;

import com.MediHubAPI.model.enums.MergeStrategy;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CopyLastWeekRequest(
        @NotNull Long doctorId,
        LocalDate targetWeekStartISO,
        MergeStrategy mergeStrategy,
        Boolean includeBlocks,
        Boolean includeDayOffFlags
) {}
