package com.MediHubAPI.dto.scheduling.session.draft;

import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleDayPlanDTO;
import com.MediHubAPI.model.enums.ScheduleMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record DraftRequest(
        Long scheduleId,   // if null -> create new draft; else update existing draft

        Long version,      // required when updating

        @NotNull ScheduleMode mode,

        Long doctorId,
        Long departmentId,

        @NotNull LocalDate weekStartDate,

        @NotNull @Min(1) @Max(240) Integer slotDurationMin,

        boolean locked,
        String lockedReason,

        @Valid
        @NotNull
        @Size(min = 1, max = 7)
        List<SessionScheduleDayPlanDTO> days
) {
    @AssertTrue(message = "version is required when scheduleId is provided")
    public boolean versionRule() {
        return scheduleId == null || version != null;
    }

    @AssertTrue(message = "doctorId is required when mode=DOCTOR_OVERRIDE")
    public boolean doctorRule() {
        if (mode != null && mode == ScheduleMode.DOCTOR_OVERRIDE) {
            return doctorId != null;
        }
        return true;
    }
}
