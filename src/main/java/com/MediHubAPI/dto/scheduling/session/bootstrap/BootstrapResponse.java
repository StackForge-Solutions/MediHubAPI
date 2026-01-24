package com.MediHubAPI.dto.scheduling.session.bootstrap;

import com.MediHubAPI.dto.scheduling.session.search.SessionScheduleSummaryDTO;

import java.time.LocalDate;
import java.util.List;

public record BootstrapResponse(
        List<DoctorLiteDTO> doctors,
        List<DepartmentLiteDTO> departments,
        List<HolidayDTO> holidays,
        List<TemplateLiteDTO> templates,   // Not implemented in STEP-1 -> empty list
        List<SessionScheduleSummaryDTO> seedSchedules,
        LocalDate serverDate
) {}
