package com.MediHubAPI.dto.scheduling.template.plan;

import java.time.DayOfWeek;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.MediHubAPI.dto.scheduling.session.template.plan.TemplateIntervalDTO;
import com.MediHubAPI.model.scheduling.template.plan.TemplateBlockDTO;

public record TemplateDayPlanDTO(
        @NotNull DayOfWeek dayOfWeek,
        boolean dayOff,
        @Valid List<TemplateIntervalDTO> intervals,
        @Valid List<TemplateBlockDTO> blocks
) {}
