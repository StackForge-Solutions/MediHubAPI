package com.MediHubAPI.dto.scheduling.session.publish;

import com.MediHubAPI.model.enums.SlotGenerationMode;
import jakarta.validation.constraints.NotNull;

public record PublishRequest(
        @NotNull Long scheduleId,
        @NotNull Long version,

        boolean failOnBookedConflict,
        boolean dryRun,

        SlotGenerationMode slotGenerationMode,
        String publishNote
) {}
