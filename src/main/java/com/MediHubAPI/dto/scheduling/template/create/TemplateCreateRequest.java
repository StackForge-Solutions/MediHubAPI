package com.MediHubAPI.dto.scheduling.template.create;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.MediHubAPI.dto.scheduling.template.plan.TemplateDayPlanDTO;
import com.MediHubAPI.model.enums.TemplateScope;

public record TemplateCreateRequest(
        @NotBlank String name,

        @NotNull TemplateScope scope,

        Long doctorId,
        Long departmentId,

        @NotNull @Min(1) @Max(240) Integer slotDurationMinutes,

        @NotNull Boolean active,

        /*
          Optional: create template by copying an existing SessionSchedule.
          If present, 'days' can be null/empty and will be taken from the schedule.
         */
        Long sourceScheduleId,

        @Valid List<TemplateDayPlanDTO> days
) {
}

