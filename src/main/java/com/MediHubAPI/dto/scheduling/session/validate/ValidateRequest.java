package com.MediHubAPI.dto.scheduling.session.validate;


import com.MediHubAPI.dto.scheduling.session.plan.SessionScheduleDayPlanDTO;
import com.MediHubAPI.model.enums.ScheduleMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

public record ValidateRequest(
        @NotNull ScheduleMode mode,

        Long doctorId,
        Long departmentId,

        @NotNull LocalDate weekStartDate,

        @NotNull @Min(1) @Max(240) Integer slotDurationMinutes,

        @Valid
        @NotNull
        @Size(min = 1, max = 7)
        List<SessionScheduleDayPlanDTO> days
) {
    @AssertTrue(message = "doctorId is required when mode=DOCTOR_OVERRIDE")
    public boolean isDoctorRuleOk() {
        if (mode == ScheduleMode.DOCTOR_OVERRIDE) {
            return doctorId != null;
        }
        return true;
    }
}
