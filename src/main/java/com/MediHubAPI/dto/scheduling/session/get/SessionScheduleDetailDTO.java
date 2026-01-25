package com.MediHubAPI.dto.scheduling.session.get;


import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleDayPlanDTO;
import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.List;

public record SessionScheduleDetailDTO(
        Long scheduleId,
        ScheduleMode mode,
        Long doctorId,
        Long departmentId,

        LocalDate weekStartISO,
        LocalDate weekEndISO,

        Integer slotDurationMin,
        ScheduleStatus status,
        boolean locked,
        String lockedReason,
        Long version,
        String createdBy,
        String updatedBy,
        List<SessionScheduleDayPlanDTO> days
) {}
