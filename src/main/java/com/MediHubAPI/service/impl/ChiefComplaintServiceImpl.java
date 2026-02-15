package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.ChiefComplaintDTO;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.ChiefComplaint;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.ChiefComplaintRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import com.MediHubAPI.service.ChiefComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChiefComplaintServiceImpl implements ChiefComplaintService {

    private final ChiefComplaintRepository chiefComplaintRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final AppointmentRepository appointmentRepository;
    private final ModelMapper modelMapper;

    @PersistenceContext
    private EntityManager entityManager;

    /** ------------------- UPSERT by appointmentId ------------------- */
    @Transactional
    @Override
    public List<ChiefComplaintDTO> upsertByAppointmentId(Long appointmentId, List<ChiefComplaintDTO> complaints) {
        log.info("Upserting ChiefComplaints for appointmentId={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        //  Find existing VisitSummary or create one if not exists
        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> {
                    log.info("No VisitSummary found for appointmentId={}, creating new one", appointmentId);
                    VisitSummary newVisit = VisitSummary.builder()
                            .appointment(appointment)
                            .doctor(appointment.getDoctor())
                            .patient(appointment.getPatient())
                            .visitDate(appointment.getAppointmentDate() != null
                                    ? appointment.getAppointmentDate().toString() : null)
                            .visitTime(appointment.getSlotTime() != null
                                    ? appointment.getSlotTime().toString() : null)
                            .build();
                    return visitSummaryRepository.saveAndFlush(newVisit);
                });

        //  Delete existing complaints for this visit
        List<ChiefComplaint> existing = chiefComplaintRepository.findByVisitSummary_Id(visitSummary.getId());
        if (!existing.isEmpty()) {
            log.info("Deleting {} existing chief complaints for visitSummaryId={}", existing.size(), visitSummary.getId());
            chiefComplaintRepository.deleteAll(existing);
        }

        //  Save new complaints
        List<ChiefComplaint> saved = complaints.stream()
                .map(dto -> {
                    ChiefComplaint entity = modelMapper.map(dto, ChiefComplaint.class);
                    entity.setVisitSummary(visitSummary);
                    return chiefComplaintRepository.save(entity);
                })
                .collect(Collectors.toList());

        log.info("Saved {} chief complaints for appointmentId={}", saved.size(), appointmentId);

        return saved.stream()
                .map(e -> modelMapper.map(e, ChiefComplaintDTO.class))
                .collect(Collectors.toList());
    }

    /** ------------------- GET by appointmentId ------------------- */
    @Override
    public List<ChiefComplaintDTO> getByAppointmentId(Long appointmentId) {
        log.info("Fetching ChiefComplaints for appointmentId={}", appointmentId);

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found for appointmentId: " + appointmentId));

        return chiefComplaintRepository.findByVisitSummary_Id(visitSummary.getId())
                .stream()
                .map(c -> modelMapper.map(c, ChiefComplaintDTO.class))
                .collect(Collectors.toList());
    }
}
