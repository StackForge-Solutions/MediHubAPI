package com.MediHubAPI.dto.scheduling.session.copy;

import java.util.List;

public record CopyFromWeekResponse(
        Long scheduleId,
        Long version,
        boolean copied,
        int skippedConflicts,
        List<String> warnings
) {}
