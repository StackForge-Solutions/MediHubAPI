package com.MediHubAPI.dto.scheduling.session.draft;

public record DraftResponse(
        Long scheduleId,
        Long newVersion,
        String message
) {}
