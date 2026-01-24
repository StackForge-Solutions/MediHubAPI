package com.MediHubAPI.scheduling.session.service.port.payload;


import com.MediHubAPI.model.enums.BlockType;
import com.MediHubAPI.model.enums.SessionType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SlotPublishCommand(
        Long scheduleId,
        Long doctorId,
        LocalDate weekStartDate,
        int slotDurationMinutes,
        boolean failOnBookedConflict,
        List<DesiredSlot> desiredSlots,
        String actor
) {
    public record DesiredSlot(
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            boolean blocked,
            BlockType blockType,
            SessionType sessionType,
            Integer capacity,
            String slotKey
    ) {}
}
