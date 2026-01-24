package com.MediHubAPI.dto.scheduling.session.publish;

public record PublishConflictDTO(
        String slotKey,   // yyyy-MM-dd|HH:mm|HH:mm
        String reason     // BOOKED_CONFLICT / LOCKED / etc.
) {}
