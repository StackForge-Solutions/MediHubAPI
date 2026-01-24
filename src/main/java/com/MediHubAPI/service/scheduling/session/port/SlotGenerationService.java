package com.MediHubAPI.service.scheduling.session.port;

import com.MediHubAPI.dto.scheduling.session.preview.PreviewSlotsResponse;
import com.MediHubAPI.model.scheduling.session.SessionSchedule;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishResult;

public interface SlotGenerationService {

    PreviewSlotsResponse preview(SessionSchedule schedule);

    SlotPublishResult publish(SessionSchedule schedule, boolean failOnBookedConflict, boolean dryRun, String actor);
}
