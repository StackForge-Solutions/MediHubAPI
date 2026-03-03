package com.MediHubAPI.service;

import com.MediHubAPI.dto.VitalsDTO;
import com.MediHubAPI.model.Vitals;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.VitalsRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VitalsService {

    private final VitalsRepository vitalsRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final ModelMapper modelMapper;

    /** Create or Update Vitals for a VisitSummary */
    @Transactional
    public VitalsDTO saveOrUpdateVitals(Long appointmentId, VitalsDTO dto) {
        log.info("Saving/Updating vitals for appointmentId={}", appointmentId);

        //  Find VisitSummary by appointmentId
        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("VisitSummary not found for appointmentId " + appointmentId));

        //  Map DTO → Entity
        Vitals vitals = modelMapper.map(dto, Vitals.class);
        vitals.setVisitSummary(visitSummary);

        //  Preserve existing Vitals ID if updating
        if (visitSummary.getVitals() != null) {
            vitals.setId(visitSummary.getVitals().getId());
        }

        //  Save and associate
        Vitals saved = vitalsRepository.save(vitals);
        visitSummary.setVitals(saved);

        log.info("Vitals saved for VisitSummary ID={}, Appointment ID={}", visitSummary.getId(), appointmentId);
        return modelMapper.map(saved, VitalsDTO.class);
    }


    /** Get Vitals by Appointment ID */
    @Transactional(readOnly = true)
    public VitalsDTO getVitalsByAppointmentId(Long appointmentId) {
        log.info("Fetching vitals for appointmentId={}", appointmentId);

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("VisitSummary not found for appointmentId " + appointmentId));

        if (visitSummary.getVitals() == null) {
            throw new IllegalStateException("Vitals not found for Appointment ID " + appointmentId);
        }

        return modelMapper.map(visitSummary.getVitals(), VitalsDTO.class);
    }

}
