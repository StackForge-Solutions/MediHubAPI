package com.MediHubAPI.dto.scheduling.session.draft;

import java.util.List;

public record DraftResponse(
        Long scheduleId,
        Long newVersion,
        String message,
        List<DraftDayStatusBadgeDTO> dayStatusBadges
) {}
