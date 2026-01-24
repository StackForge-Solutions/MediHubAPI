package com.MediHubAPI.dto.scheduling.session.preview;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

public record PreviewDayDTO(
        DayOfWeek dayOfWeek,
        LocalDate date,
        boolean dayOff,
        List<PreviewSlotCellDTO> slots
) {}
