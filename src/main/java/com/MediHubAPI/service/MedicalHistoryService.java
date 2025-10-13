package com.MediHubAPI.service;

import com.MediHubAPI.dto.MedicalHistoryDTO;
import com.MediHubAPI.model.MedicalHistory;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.MedicalHistoryRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryService {

    private final MedicalHistoryRepository medicalHistoryRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** ------------------- Save or Update by Appointment ID ------------------- */
    @Transactional
    public MedicalHistoryDTO saveOrUpdateByAppointment(Long appointmentId, MedicalHistoryDTO dto) {
        log.info("Saving/Updating medical history for appointmentId={}", appointmentId);

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("VisitSummary not found for appointmentId " + appointmentId));

        // Map DTO → Entity
        MedicalHistory history = modelMapper.map(dto, MedicalHistory.class);
        history.setVisitSummary(visitSummary);

        // Update if existing
        if (visitSummary.getMedicalHistory() != null) {
            history.setId(visitSummary.getMedicalHistory().getId());
        }

        MedicalHistory saved = medicalHistoryRepository.save(history);
        visitSummary.setMedicalHistory(saved);

        return modelMapper.map(saved, MedicalHistoryDTO.class);
    }

    /** ------------------- Get by Appointment ID ------------------- */
    @Transactional(readOnly = true)
    public MedicalHistoryDTO getByAppointmentId(Long appointmentId) {
        log.info("Fetching medical history for appointmentId={}", appointmentId);

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("VisitSummary not found for appointmentId " + appointmentId));

        if (visitSummary.getMedicalHistory() == null) {
            throw new IllegalStateException("No medical history recorded for appointmentId " + appointmentId);
        }

        return modelMapper.map(visitSummary.getMedicalHistory(), MedicalHistoryDTO.class);
    }
}
