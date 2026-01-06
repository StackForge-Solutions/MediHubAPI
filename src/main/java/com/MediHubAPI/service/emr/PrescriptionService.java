package com.MediHubAPI.service.emr;

import com.MediHubAPI.dto.emr.PrescriptionFetchResponse;
import com.MediHubAPI.dto.emr.PrescriptionSaveRequest;
import com.MediHubAPI.dto.emr.PrescriptionSaveResponse;

public interface PrescriptionService {
    PrescriptionSaveResponse saveOrUpdate(Long appointmentId, PrescriptionSaveRequest request);
    PrescriptionFetchResponse getByAppointmentId(Long appointmentId);
}
