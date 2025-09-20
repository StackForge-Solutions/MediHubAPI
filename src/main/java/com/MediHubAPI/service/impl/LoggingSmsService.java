package com.MediHubAPI.service.impl;

import com.MediHubAPI.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@Service
@ConditionalOnProperty(name = "sms.provider", havingValue = "log")
public class LoggingSmsService implements SmsService {
    @Override
    public void notifyAppointmentRescheduled(Long appointmentId, Long patientUserId,
                                             LocalDate date, LocalTime newStart, String reason) {
        log.info("SMS[SIMULATED] apptId={}, patientId={}, date={}, time={}, reason={}",
                appointmentId, patientUserId, date, newStart, reason);
    }
}
