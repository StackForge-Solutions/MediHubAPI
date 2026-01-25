package com.MediHubAPI.dto.scheduling.session.bootstrap;


import java.util.List;

public record WeeklyTemplateDTO(
        String weekStartISO,
        String weekEndISO,
        String departmentId,
        String timezone,
        Integer slotDurationMin,
        Long version,
        List<WeeklyDayDTO> days
) {}
