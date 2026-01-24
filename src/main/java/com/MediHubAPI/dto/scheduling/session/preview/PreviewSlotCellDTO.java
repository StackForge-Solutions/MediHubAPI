package com.MediHubAPI.dto.scheduling.session.preview;

import com.MediHubAPI.model.enums.BlockType;
import com.MediHubAPI.model.enums.SessionType;

import java.time.LocalTime;

public record PreviewSlotCellDTO(
        String slotKey,           // yyyy-MM-dd|HH:mm|HH:mm
        LocalTime startTime,
        LocalTime endTime,
        boolean blocked,
        BlockType blockType,
        SessionType sessionType,
        Integer capacity
) {}
