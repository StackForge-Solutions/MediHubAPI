package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.PrescribedTestDTO;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.PrescribedTest;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.PrescribedTestRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import com.MediHubAPI.service.PrescribedTestService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescribedTestServiceImpl implements PrescribedTestService {

    private final PrescribedTestRepository prescribedTestRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final AppointmentRepository appointmentRepository;
    private final ModelMapper modelMapper;
    @PersistenceContext
    private EntityManager entityManager; // needed to initialize proxies

    @Transactional
    @Override
    public List<PrescribedTestDTO> saveOrUpdateTests(Long appointmentId, List<PrescribedTestDTO> testDTOs) {
        log.info("Upserting PrescribedTests for appointmentId={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found for appointmentId: " + appointmentId));

        // 1️⃣ Delete existing tests first
        List<PrescribedTest> existing = prescribedTestRepository.findByVisitSummary_Id(visitSummary.getId());
        if (!existing.isEmpty()) {
            log.info("Deleting {} existing PrescribedTests for visitSummaryId={}", existing.size(), visitSummary.getId());
            prescribedTestRepository.deleteAllInBatch(existing); // ✅ Faster + consistent
            prescribedTestRepository.flush();                   // ✅ Force deletion to DB immediately
            entityManager.clear();                              // ✅ Clear persistence context
        }

        // 2️⃣ Insert new tests
        List<PrescribedTest> newTests = testDTOs.stream()
                .map(dto -> {
                    PrescribedTest test = modelMapper.map(dto, PrescribedTest.class);
                    test.setId(null);                           // ✅ Ensure treated as new entity
                    test.setVisitSummary(visitSummary);
                    return test;
                })
                .collect(Collectors.toList());

        List<PrescribedTest> saved = prescribedTestRepository.saveAll(newTests);
        prescribedTestRepository.flush();

        log.info("Saved {} new PrescribedTests for appointmentId={}", saved.size(), appointmentId);

        return saved.stream()
                .map(t -> modelMapper.map(t, PrescribedTestDTO.class))
                .collect(Collectors.toList());
    }


    @Override
    public List<PrescribedTestDTO> getTestsByAppointmentId(Long appointmentId) {
        log.info("Fetching prescribed tests for appointmentId={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found for appointmentId: " + appointmentId));

        return prescribedTestRepository.findByVisitSummary_Id(visitSummary.getId()).stream()
                .map(test -> modelMapper.map(test, PrescribedTestDTO.class))
                .collect(Collectors.toList());
    }
}
