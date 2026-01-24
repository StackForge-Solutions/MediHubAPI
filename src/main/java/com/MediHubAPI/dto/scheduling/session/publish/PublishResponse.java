package com.MediHubAPI.dto.scheduling.session.publish;

import java.util.List;

public record PublishResponse(
        Long scheduleId,
        Long newVersion,
        boolean published,
        int totalPlanned,
        int created,
        int updated,
        int skipped,
        List<PublishConflictDTO> conflicts,
        String message
) {}
