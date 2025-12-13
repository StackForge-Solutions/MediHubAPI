package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.VisitSummaryDTO;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import com.MediHubAPI.service.VisitSummaryService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitSummaryServiceImpl implements VisitSummaryService {


    private final VisitSummaryRepository visitSummaryRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @PersistenceContext
    private EntityManager entityManager; // ✅ needed to initialize proxies


    @Transactional
    @Override
    public VisitSummaryDTO upsertByAppointment(Long appointmentId) {
        log.info("Upsert VisitSummary for appointmentId={}", appointmentId);

        // Fetch appointment
        Appointment appointment = entityManager.find(Appointment.class, appointmentId);
        if (appointment == null) {
            throw new RuntimeException("Appointment not found for ID: " + appointmentId);
        }

        // Try existing VisitSummary
        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElse(null);

        if (visitSummary != null) {
            log.info("Existing VisitSummary found id={} for appointmentId={}", visitSummary.getId(), appointmentId);
            return modelMapper.map(visitSummary, VisitSummaryDTO.class);
        }

        // If not exists → create new VisitSummary record linked to existing entities
        visitSummary = VisitSummary.builder()
                .appointment(appointment)
                .doctor(appointment.getDoctor())
                .patient(appointment.getPatient())
                .visitDate(appointment.getAppointmentDate() != null
                        ? appointment.getAppointmentDate().toString()
                        : null)
                .visitTime(appointment.getSlotTime() != null
                        ? appointment.getSlotTime().toString()
                        : null)
                .build();

        visitSummaryRepository.saveAndFlush(visitSummary);
        entityManager.refresh(visitSummary);

        log.info("New VisitSummary created id={} for appointmentId={}", visitSummary.getId(), appointmentId);

        return modelMapper.map(visitSummary, VisitSummaryDTO.class);
    }

    @Override
    public VisitSummaryDTO getByAppointmentId(Long appointmentId) {
        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("Visit Summary not found for appointmentId: " + appointmentId));

        return modelMapper.map(visitSummary, VisitSummaryDTO.class);
    }

}
