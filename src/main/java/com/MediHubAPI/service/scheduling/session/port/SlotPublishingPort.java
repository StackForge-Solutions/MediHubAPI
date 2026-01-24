package com.MediHubAPI.service.scheduling.session.port;

import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishCommand;
import com.MediHubAPI.scheduling.session.service.port.payload.SlotPublishResult;

public interface SlotPublishingPort {

    /**
     * TODO (YOU WILL IMPLEMENT):
     * Connect to existing SlotRepository/SlotServiceImpl and enforce:
     * - idempotent create by (doctorId, date, startTime, endTime)
     * - never delete/modify booked slots (slot.getAppointment()!=null)
     * - if blocking overlaps booked and failOnBookedConflict=true -> return conflict
     */
    SlotPublishResult publishSlots(SlotPublishCommand command);
}
