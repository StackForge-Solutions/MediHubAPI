package com.MediHubAPI.service.emr.impl;

import com.MediHubAPI.dto.emr.importprev.*;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.PrescribedTest;
import com.MediHubAPI.model.emr.Prescription;
import com.MediHubAPI.model.emr.PrescriptionMedication;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.PrescribedTestRepository;
import com.MediHubAPI.repository.emr.PrescriptionRepository;
import com.MediHubAPI.service.emr.EmrPreviousPrescriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmrPreviousPrescriptionServiceImpl implements EmrPreviousPrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PrescribedTestRepository prescribedTestRepository;

    @Override
    @Transactional(readOnly = true)
    public PreviousPrescriptionsDataDto byPatient(Long patientId, Integer limit) {

        int safeLimit = normalizeLimit(limit);
        List<Prescription> pres = prescriptionRepository.findLatestByPatient(patientId, PageRequest.of(0, safeLimit));

        List<PreviousPrescriptionItemDto> items = mapToItemsWithTests(pres);

        return PreviousPrescriptionsDataDto.builder()
                .items(items)
                .count(items.size())
                .limit(safeLimit)
                .patientId(patientId)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PreviousPrescriptionsDataDto byAppointment(Long appointmentId, Integer limit) {

        int safeLimit = normalizeLimit(limit);

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        Long patientId = appt.getPatient().getId();

        List<Prescription> pres = prescriptionRepository.findPreviousByPatientCursor(
                patientId,
                appt.getAppointmentDate(),
                appt.getId(),
                PageRequest.of(0, safeLimit)
        );

        List<PreviousPrescriptionItemDto> items = mapToItemsWithTests(pres);

        return PreviousPrescriptionsDataDto.builder()
                .items(items)
                .count(items.size())
                .limit(safeLimit)
                .appointmentId(appointmentId)
                .build();
    }

    private int normalizeLimit(Integer limit) {
        int v = (limit == null || limit <= 0) ? 5 : limit;
        return Math.min(v, 20);
    }

    private List<PreviousPrescriptionItemDto> mapToItemsWithTests(List<Prescription> pres) {

        // bulk load prescribed_tests for all visit summaries
        List<Long> vsIds = pres.stream()
                .map(p -> p.getVisitSummary().getId())
                .distinct()
                .collect(Collectors.toList());

        Map<Long, List<PrescribedTest>> testsByVsId = new HashMap<>();
        if (!vsIds.isEmpty()) {
            List<PrescribedTest> tests = prescribedTestRepository.findByVisitSummary_IdIn(vsIds);
            testsByVsId = tests.stream().collect(Collectors.groupingBy(t -> t.getVisitSummary().getId()));
        }

        List<PreviousPrescriptionItemDto> out = new ArrayList<>(pres.size());

        for (Prescription p : pres) {

            Appointment a = p.getAppointment();

            String doctorName = a.getDoctor() == null
                    ? null
                    : (a.getDoctor().getFirstName() + " " + a.getDoctor().getLastName()).trim();

            PrescriptionPayloadDto payload = PrescriptionPayloadDto.builder()
                    .language(p.getLanguage())
                    .followUpEnabled(p.getFollowUpEnabled())
                    .followUpDuration(p.getFollowUpDuration())
                    .followUpUnit(p.getFollowUpUnit())
                    .followUpDate(p.getFollowUpDate())
                    .sendFollowUpEmail(p.getSendFollowUpEmail())
                    .adviceText(p.getAdviceText())
                    .medications(mapMeds(p.getMedications()))
                    .tests(mapTests(testsByVsId.getOrDefault(p.getVisitSummary().getId(), List.of())))
                    .build();

            out.add(PreviousPrescriptionItemDto.builder()
                    .appointmentId(a.getId())
                    .prescriptionId(p.getId())
                    .visitDate(a.getAppointmentDate())
                    .doctorId(a.getDoctor() == null ? null : a.getDoctor().getId())
                    .doctorName(doctorName)
                    .summary("") // if you have VisitSummary text, plug it here
                    .payload(payload)
                    .build());
        }

        return out;
    }

    private List<PrescriptionPayloadDto.MedicationDto> mapMeds(List<PrescriptionMedication> meds) {
        if (meds == null) return List.of();
        return meds.stream()
                .map(m -> PrescriptionPayloadDto.MedicationDto.builder()
                        .id(m.getId())
                        .medicineId(m.getMedicineId())
                        .form(m.getForm())
                        .mode(m.getMode())
                        .medicineName(m.getMedicineName())
                        .composition(m.getComposition())
                        .inStock(m.getInStock())
                        .stockQty(m.getStockQty())
                        .m(m.getM())
                        .a(m.getA())
                        .n(m.getN())
                        .sosEnabled(m.getSosEnabled())
                        .sosCount(m.getSosCount())
                        .sosUnit(m.getSosUnit())
                        .startDate(m.getStartDate())
                        .lifelong(m.getLifelong())
                        .duration(m.getDuration())
                        .durationUnit(m.getDurationUnit())
                        .periodicity(m.getPeriodicity())
                        .build())
                .collect(Collectors.toList());
    }

    private List<PrescriptionPayloadDto.TestDto> mapTests(List<PrescribedTest> tests) {
        if (tests == null) return List.of();
        return tests.stream()
                .map(t -> PrescriptionPayloadDto.TestDto.builder()
                        .id(t.getPathologyTestMaster() == null ? null : t.getPathologyTestMaster().getId())
                        .name(t.getName())
                        .price(t.getPrice())
                        .tat(t.getTat())
                        .quantity(t.getQuantity())
                        .notes(t.getNotes())
                        .isCustom(t.getPathologyTestMaster() == null)
                        .build())
                .collect(Collectors.toList());
    }
}
