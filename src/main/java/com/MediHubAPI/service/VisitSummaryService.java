package com.MediHubAPI.service;

import com.MediHubAPI.dto.VisitSummaryDTO;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitSummaryService {

    private final VisitSummaryRepository visitSummaryRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @PersistenceContext
    private EntityManager entityManager; // ✅ needed to initialize proxies

    @Transactional
    public VisitSummaryDTO saveVisitSummary(Long doctorId, Long patientId, VisitSummary visitSummary) {
        log.info("Saving VisitSummary for patientId: {} and doctorId: {}", patientId, doctorId);

        User doctor = userRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        // ✅ Attach relationships
        visitSummary.setDoctor(doctor);
        visitSummary.setPatient(patient);

        if (visitSummary.getChiefComplaints() != null) {
            visitSummary.getChiefComplaints().forEach(c -> c.setVisitSummary(visitSummary));
        }

        VisitSummary saved = visitSummaryRepository.saveAndFlush(visitSummary);

        // ✅ Force initialize lazy associations to prevent proxy reflection error
        entityManager.refresh(saved); // ensures the entity and relations are fully managed
        saved.setDoctor(doctor);
        saved.setPatient(patient);

        // ✅ Optionally detach from persistence context to avoid further proxy wrapping
        entityManager.detach(saved);
        entityManager.detach(doctor);
        entityManager.detach(patient);

        log.info("Saved VisitSummary with ID: {}", saved.getId());

        // ✅ Now safe to map
        return modelMapper.map(saved, VisitSummaryDTO.class);
    }

    @Transactional(readOnly = true)
    public VisitSummaryDTO getVisitSummaryById(Long id) {
        VisitSummary visit = visitSummaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found"));

        // force initialize associations before mapping
        entityManager.refresh(visit);

        return modelMapper.map(visit, VisitSummaryDTO.class);
    }

    @Transactional
    public VisitSummaryDTO updateVisitSummary(Long id, VisitSummary visitSummary) {
        VisitSummary updated = visitSummaryRepository.findById(id).map(existing -> {
            existing.setVisitDate(visitSummary.getVisitDate());
            existing.setVisitTime(visitSummary.getVisitTime());

            if (visitSummary.getChiefComplaints() != null) {
                existing.getChiefComplaints().clear();
                visitSummary.getChiefComplaints().forEach(c -> c.setVisitSummary(existing));
                existing.getChiefComplaints().addAll(visitSummary.getChiefComplaints());
            }

            return visitSummaryRepository.saveAndFlush(existing);
        }).orElseThrow(() -> new RuntimeException("VisitSummary not found"));

        entityManager.refresh(updated);
        return modelMapper.map(updated, VisitSummaryDTO.class);
    }
}
