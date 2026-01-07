package com.MediHubAPI.service.emr;

import com.MediHubAPI.dto.emr.importprev.PreviousPrescriptionsDataDto;

public interface EmrPreviousPrescriptionService {
    PreviousPrescriptionsDataDto byPatient(Long patientId, Integer limit);
    PreviousPrescriptionsDataDto byAppointment(Long appointmentId, Integer limit);
}
