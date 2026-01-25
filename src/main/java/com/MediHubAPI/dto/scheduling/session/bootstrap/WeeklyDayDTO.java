package com.MediHubAPI.dto.scheduling.session.bootstrap;


import java.util.List;

public record WeeklyDayDTO(
        Integer weekday,        // 1..7
        String dateISO,         // yyyy-MM-dd
        boolean isDayOff,
        List<WeeklySessionDTO> sessions,

        String notes
) {}
