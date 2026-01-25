package com.MediHubAPI.dto.scheduling.template.get;

import java.time.Instant;
import java.util.List;
import com.MediHubAPI.dto.scheduling.template.plan.TemplateDayPlanDTO;
import com.MediHubAPI.model.enums.TemplateScope;

public record TemplateDetailDTO(
        Long id,
        TemplateScope scope,
        Long doctorId,
        Long departmentId,
        String name,
        Integer slotDurationMin,
        boolean active,
        Long version,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt,
        List<TemplateDayPlanDTO> days
) {}
