package com.MediHubAPI.service;

import com.MediHubAPI.dto.MedicalHistoryDTO;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.model.MedicalHistory;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.MedicalHistoryRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryService {

    private final MedicalHistoryRepository medicalHistoryRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final ObjectMapper objectMapper;

    /** ------------------- Save or Update by Appointment ID ------------------- */
    @Transactional
    public MedicalHistoryDTO saveOrUpdateByAppointment(Long appointmentId, MedicalHistoryDTO dto) {
        log.info("Saving/Updating medical history for appointmentId={}", appointmentId);

        validateAppointmentId(appointmentId);
        if (dto == null) {
            throw validation("payload", "Request body is required");
        }

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("VisitSummary not found for appointmentId " + appointmentId));

        MedicalHistory history = visitSummary.getMedicalHistory() != null
                ? visitSummary.getMedicalHistory()
                : new MedicalHistory();

        history.setPersonalHistory(toJson(dto.getPersonalHistory()));
        history.setRenalHistory(toJson(dto.getRenalHistory()));
        history.setDiabetesHistory(toJson(dto.getDiabetesHistory()));
        history.setPastHistory(toJson(dto.getPastHistory()));
        history.setOtherHistories(toJson(dto.getOtherHistories()));
        history.setVisitSummary(visitSummary);

        MedicalHistory saved = medicalHistoryRepository.save(history);
        visitSummary.setMedicalHistory(saved);

        return toDto(saved);
    }

    /** ------------------- Get by Appointment ID ------------------- */
    @Transactional(readOnly = true)
    public MedicalHistoryDTO getByAppointmentId(Long appointmentId) {
        log.info("Fetching medical history for appointmentId={}", appointmentId);

        validateAppointmentId(appointmentId);

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("VisitSummary not found for appointmentId " + appointmentId));

        if (visitSummary.getMedicalHistory() == null) {
            throw new IllegalStateException("No medical history recorded for appointmentId " + appointmentId);
        }

        return toDto(visitSummary.getMedicalHistory());
    }

    private MedicalHistoryDTO toDto(MedicalHistory entity) {
        return new MedicalHistoryDTO(
                entity.getId(),
                fromJson(entity.getPersonalHistory()),
                fromJson(entity.getRenalHistory()),
                fromJson(entity.getDiabetesHistory()),
                fromJson(entity.getPastHistory()),
                fromJson(entity.getOtherHistories())
        );
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw validation("payload", "Invalid medical history payload: " + e.getOriginalMessage());
        }
    }

    private Object fromJson(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse stored medical history JSON", e);
        }
    }

    private void validateAppointmentId(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            throw validation("appointmentId", "appointmentId must be a positive number");
        }
    }

    private ValidationException validation(String field, String message) {
        return new ValidationException(
                "Validation failed",
                java.util.List.of(new ValidationException.ValidationErrorDetail(field, message))
        );
    }
}
