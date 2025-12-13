package com.MediHubAPI.service;

import com.MediHubAPI.dto.PrescribedTestDTO;

import java.util.List;

public interface PrescribedTestService {
    List<PrescribedTestDTO> saveOrUpdateTests(Long appointmentId, List<PrescribedTestDTO> tests);
    List<PrescribedTestDTO> getTestsByAppointmentId(Long appointmentId);
}
