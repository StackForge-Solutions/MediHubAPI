package com.MediHubAPI.dto.scheduling.session.search;


import com.MediHubAPI.model.enums.ScheduleMode;
import com.MediHubAPI.model.enums.ScheduleStatus;

import java.time.LocalDate;

public record SessionScheduleSummaryDTO(
        Long id,
        ScheduleMode mode,
        Long doctorId,
        Long departmentId,
        LocalDate weekStartDate,
        ScheduleStatus status,
        boolean locked,
        Long version
) {}
