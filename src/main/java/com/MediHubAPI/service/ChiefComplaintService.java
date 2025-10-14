package com.MediHubAPI.service;

import com.MediHubAPI.dto.ChiefComplaintDTO;
import java.util.List;

public interface ChiefComplaintService {
    List<ChiefComplaintDTO> upsertByAppointmentId(Long appointmentId, List<ChiefComplaintDTO> complaints);
    List<ChiefComplaintDTO> getByAppointmentId(Long appointmentId);
}
