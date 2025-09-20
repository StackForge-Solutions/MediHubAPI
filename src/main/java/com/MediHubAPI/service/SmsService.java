package com.MediHubAPI.service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Contract for sending SMS notifications.
 */
public interface SmsService {

    /**
     * Notify patient that appointment has been rescheduled.
     * Implementation should lookup patient contact or accept phone if you prefer.
     */
    void notifyAppointmentRescheduled(Long appointmentId,
                                      Long patientUserId,
                                      LocalDate date,
                                      LocalTime newStart,
                                      String reason);
}
