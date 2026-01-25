package com.MediHubAPI.dto.scheduling.session.bootstrap;


import com.MediHubAPI.model.enums.ScheduleMode;

import java.util.List;

public record SeedWeeklyScheduleDTO(
        Long scheduleId,
        ScheduleMode mode,

        String weekStartISO,
        String weekEndISO,

        String departmentId,   // null or "2" etc
        Long templateId,       // global template schedule id
        Long doctorId,         // only for override

        String timezone,
        Integer slotDurationMin,

        Boolean inheritTemplate, // only for override
        Long version,

        List<WeeklyDayDTO> days,
        String notes
) {}
