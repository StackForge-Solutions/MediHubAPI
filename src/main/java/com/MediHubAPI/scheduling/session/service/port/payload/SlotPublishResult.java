package com.MediHubAPI.scheduling.session.service.port.payload;

import java.util.List;

public record SlotPublishResult(
        int totalPlanned,
        int created,
        int updated,
        int skipped,
        List<Conflict> conflicts
) {
    public record Conflict(
            String slotKey,
            String reason
    ) {}
}
