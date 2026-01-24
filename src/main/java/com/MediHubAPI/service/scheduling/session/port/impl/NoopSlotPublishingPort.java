package com.MediHubAPI.service.scheduling.session.port.impl;


import com.MediHubAPI.service.scheduling.session.port.SlotPublishingPort;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishCommand;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@ConditionalOnMissingBean(SlotPublishingPort.class)
public class NoopSlotPublishingPort implements SlotPublishingPort {

    @Override
    public SlotPublishResult publishSlots(SlotPublishCommand command) {
        // This prevents silent data loss. You MUST implement SlotPublishingPort to publish into Slot table safely.
        var conflicts = command.desiredSlots().stream()
                .map(s -> new SlotPublishResult.Conflict(s.slotKey(), "SLOT_PUBLISHING_PORT_NOT_IMPLEMENTED"))
                .collect(Collectors.toList());

        return new SlotPublishResult(
                command.desiredSlots().size(),
                0,
                0,
                command.desiredSlots().size(),
                conflicts
        );
    }
}
