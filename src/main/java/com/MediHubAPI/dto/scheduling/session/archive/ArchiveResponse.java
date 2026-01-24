package com.MediHubAPI.dto.scheduling.session.archive;


public record ArchiveResponse(
        Long scheduleId,
        Long newVersion,
        String message
) {}
