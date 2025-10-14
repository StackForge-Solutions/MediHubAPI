package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.PrescribedTestDTO;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.PrescribedTest;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.model.mdm.PathologyTestMaster;
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

    @Override
    @Transactional
    public List<PrescribedTestDTO> saveOrUpdateTests(Long appointmentId, List<PrescribedTestDTO> testDTOs) {
        log.info("Upserting PrescribedTests for appointmentId={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found for appointmentId: " + appointmentId));

        // 1️⃣ Delete existing first (if any)
        List<PrescribedTest> existing = prescribedTestRepository.findByVisitSummary_Id(visitSummary.getId());
        prescribedTestRepository.deleteAllInBatch(existing);
        prescribedTestRepository.flush();
        entityManager.clear();

        // 2️⃣ Map new tests
        List<PrescribedTest> newTests = testDTOs.stream()
                .map(dto -> {
                    PrescribedTest entity = new PrescribedTest();
                    entity.setVisitSummary(visitSummary);

                    if (dto.getMasterTestId() != null) {
                        PathologyTestMaster master = entityManager.find(PathologyTestMaster.class, dto.getMasterTestId());
                        if (master != null) {
                            entity.setPathologyTestMaster(master);
                            entity.setName(master.getName());
                            entity.setPrice(master.getPrice());
                            entity.setTat(master.getTat());
//                            entity.setIsCustom(false);
                        }
                    } else {
                        // Custom test
                        entity.setName(dto.getName());
                        entity.setPrice(dto.getPrice());
                        entity.setTat(dto.getTat());
//                        entity.setIsCustom(true);
                    }

                    entity.setQuantity(dto.getQuantity());
                    entity.setNotes(dto.getNotes());
                    return entity;
                })
                .collect(Collectors.toList());

        List<PrescribedTest> saved = prescribedTestRepository.saveAllAndFlush(newTests);

        return saved.stream()
                .map(test -> modelMapper.map(test, PrescribedTestDTO.class))
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
