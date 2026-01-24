package com.MediHubAPI.dto.scheduling.template.list;


import com.MediHubAPI.model.enums.TemplateScope;

public record TemplateSummaryDTO(
        Long id,
        TemplateScope scope,
        Long doctorId,
        Long departmentId,
        String name,
        Integer slotDurationMinutes,
        boolean active,
        Long version
) {}

