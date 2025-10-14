package com.MediHubAPI.service.impl;

import com.MediHubAPI.dto.PrescribedTestDTO;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.PrescribedTest;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.PrescribedTestRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import com.MediHubAPI.service.PrescribedTestService;
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

    @Transactional
    @Override
    public List<PrescribedTestDTO> saveOrUpdateTests(Long appointmentId, List<PrescribedTestDTO> testDTOs) {
        log.info("Saving or updating PrescribedTests for appointmentId={}", appointmentId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found for appointmentId: " + appointmentId));

        // Delete existing tests (upsert behavior)
        prescribedTestRepository.findByVisitSummary_Id(visitSummary.getId())
                .forEach(existing -> prescribedTestRepository.delete(existing));

        // Save new tests
        List<PrescribedTest> savedTests = testDTOs.stream()
                .map(dto -> {
                    PrescribedTest test = modelMapper.map(dto, PrescribedTest.class);
                    test.setVisitSummary(visitSummary);
                    return prescribedTestRepository.save(test);
                })
                .collect(Collectors.toList());

        log.info("Saved {} prescribed tests for appointmentId={}", savedTests.size(), appointmentId);
        return savedTests.stream()
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
