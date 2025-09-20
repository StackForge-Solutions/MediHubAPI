package com.MediHubAPI.service;

public interface SmsService {
    void notifyAppointmentRescheduled(Long appointmentId, Long patientUserId,
                                      java.time.LocalDate date, java.time.LocalTime newStart,
                                      String reason);

    static SmsService noop() {
        return (a, b, c, d, e) -> {
        };
    }
}
