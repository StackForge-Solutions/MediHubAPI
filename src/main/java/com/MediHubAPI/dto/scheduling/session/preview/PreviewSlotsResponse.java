package com.MediHubAPI.dto.scheduling.session.preview;

import java.time.LocalDate;
import java.util.List;

public record PreviewSlotsResponse(
        LocalDate weekStartDate,
        int slotDurationMin,
        List<PreviewDayDTO> days,
        int totalSlotsPlanned
) {}
