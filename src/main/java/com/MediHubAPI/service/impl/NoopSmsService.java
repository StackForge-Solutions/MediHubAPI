package com.MediHubAPI.service.impl;

import com.MediHubAPI.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * No-op SMS: logs only, used when sms.enabled=false
 */
@Slf4j
@Service("noopSmsService")
public class NoopSmsService implements SmsService {
    @Override
    public void notifyAppointmentRescheduled(Long appointmentId, Long patientUserId,
                                             java.time.LocalDate date, java.time.LocalTime newStart,
                                             String reason) {
        log.info("[SMS:NOOP] apptId={}, patientUserId={}, date={}, time={}, reason={}",
                appointmentId, patientUserId, date, newStart, reason);
    }
}
