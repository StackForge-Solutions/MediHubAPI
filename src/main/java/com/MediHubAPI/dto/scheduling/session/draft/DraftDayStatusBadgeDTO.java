package com.MediHubAPI.dto.scheduling.session.draft;

import java.time.DayOfWeek;

public record DraftDayStatusBadgeDTO(
        DayOfWeek dayOfWeek,
        String label,
        String highlightColor
) {}
