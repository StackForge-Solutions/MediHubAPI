package com.MediHubAPI.service;

import com.MediHubAPI.dto.VisitSummaryDTO;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.UserRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
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
public class VisitSummaryService {

    private final VisitSummaryRepository visitSummaryRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @PersistenceContext
    private EntityManager entityManager; // ✅ needed to initialize proxies






    public VisitSummaryDTO getVisitSummaryById(Long id) {
        VisitSummary visitSummary = visitSummaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visit Summary not found for id: " + id));

        // ✅ Use ModelMapper instead of manual mapping
        return modelMapper.map(visitSummary, VisitSummaryDTO.class);
    }

    public List<VisitSummaryDTO> searchVisitSummaries(Long patientId, Long doctorId, Long appointmentId) {
        List<VisitSummary> results = visitSummaryRepository.findByFilters(patientId, doctorId, appointmentId);

        // ✅ Map all entities to DTOs using ModelMapper
        return results.stream()
                .map(v -> modelMapper.map(v, VisitSummaryDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Save or Update VisitSummary in one method
     */
    @Transactional
    public VisitSummaryDTO saveOrUpdateVisitSummary(Long doctorId, Long patientId, Long appointmentId, VisitSummary visitSummary) {
        log.info("saveOrUpdateVisitSummary() called for doctorId={}, patientId={}, appointmentId={}", doctorId, patientId, appointmentId);

        // ✅ Fetch Doctor
        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found with ID " + doctorId));

        // ✅ Fetch Patient
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found with ID " + patientId));

        // ✅ Fetch Appointment
        Appointment appointment = entityManager.find(Appointment.class, appointmentId);
        if (appointment == null) {
            throw new RuntimeException("Appointment not found with ID " + appointmentId);
        }

        // ✅ Try to find existing VisitSummary
        VisitSummary existing = visitSummaryRepository
                .findFirstByDoctorIdAndPatientIdAndAppointmentId(doctorId, patientId, appointmentId)
                .orElse(null);

        if (existing != null) {
            log.info("Updating existing VisitSummary id={}", existing.getId());
            existing.setVisitDate(visitSummary.getVisitDate());
            existing.setVisitTime(visitSummary.getVisitTime());

            if (visitSummary.getChiefComplaints() != null) {
                existing.getChiefComplaints().clear();
                visitSummary.getChiefComplaints().forEach(c -> c.setVisitSummary(existing));
                existing.getChiefComplaints().addAll(visitSummary.getChiefComplaints());
            }

            VisitSummary updated = visitSummaryRepository.saveAndFlush(existing);
            entityManager.refresh(updated);
            VisitSummaryDTO dto = modelMapper.map(updated, VisitSummaryDTO.class);
            dto.setDoctorId(doctorId);
            dto.setPatientId(patientId);
            dto.setDoctorName(doctor.getFirstName() + " " + doctor.getLastName());
            dto.setPatientName(patient.getFirstName() + " " + patient.getLastName());
            return dto;
        }

        // ✅ Else create new
        log.info("Creating new VisitSummary record");
        visitSummary.setDoctor(doctor);
        visitSummary.setPatient(patient);
        visitSummary.setAppointment(appointment);

        if (visitSummary.getChiefComplaints() != null) {
            visitSummary.getChiefComplaints().forEach(c -> c.setVisitSummary(visitSummary));
        }

        VisitSummary saved = visitSummaryRepository.saveAndFlush(visitSummary);
        entityManager.refresh(saved);

        VisitSummaryDTO dto = modelMapper.map(saved, VisitSummaryDTO.class);
        dto.setDoctorId(doctorId);
        dto.setPatientId(patientId);
        dto.setDoctorName(doctor.getFirstName() + " " + doctor.getLastName());
        dto.setPatientName(patient.getFirstName() + " " + patient.getLastName());

        return dto;
    }

}
