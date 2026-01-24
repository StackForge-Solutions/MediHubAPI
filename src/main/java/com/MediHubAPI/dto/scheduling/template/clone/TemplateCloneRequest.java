package com.MediHubAPI.dto.scheduling.template.clone;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.MediHubAPI.model.enums.TemplateScope;

public record TemplateCloneRequest(
        @NotNull Long sourceVersion,
        @NotBlank String newName,
        @NotNull TemplateScope targetScope,
        Long targetDoctorId,
        Long targetDepartmentId,
        boolean active
) {}

