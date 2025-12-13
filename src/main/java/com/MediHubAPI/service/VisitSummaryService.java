package com.MediHubAPI.service;

import com.MediHubAPI.dto.VisitSummaryDTO;

public interface VisitSummaryService {
    VisitSummaryDTO upsertByAppointment(Long appointmentId);
    VisitSummaryDTO getByAppointmentId(Long appointmentId);
}
