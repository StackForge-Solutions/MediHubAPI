package com.MediHubAPI.service.emr.impl;

import com.MediHubAPI.dto.PrescribedTestDTO;
import com.MediHubAPI.dto.emr.*;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.model.emr.Prescription;
import com.MediHubAPI.model.emr.PrescriptionMedication;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import com.MediHubAPI.repository.emr.PrescriptionMedicationRepository;
import com.MediHubAPI.repository.emr.PrescriptionRepository;
import com.MediHubAPI.service.PrescribedTestService;
import com.MediHubAPI.service.emr.PrescriptionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionServiceImpl implements PrescriptionService {

    private static final DateTimeFormatter ISO_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionMedicationRepository medicationRepository;

    private final AppointmentRepository appointmentRepository;
    private final VisitSummaryRepository visitSummaryRepository;

    private final PrescribedTestService prescribedTestService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    @Override
    public PrescriptionSaveResponse saveOrUpdate(Long appointmentId, PrescriptionSaveRequest request) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found for appointmentId: " + appointmentId));

        Prescription prescription = prescriptionRepository.findByAppointment_Id(appointmentId)
                .orElseGet(() -> Prescription.builder()
                        .appointment(appointment)
                        .visitSummary(visitSummary)
                        .build());

        // 1) Header fields
        prescription.setLanguage(request.getLanguage());

        Boolean followUpEnabled = Boolean.TRUE.equals(request.getFollowUpEnabled());
        prescription.setFollowUpEnabled(followUpEnabled);

        prescription.setSendFollowUpEmail(Boolean.TRUE.equals(request.getSendFollowUpEmail()));
        prescription.setAdviceText(request.getAdviceText());

        if (!followUpEnabled) {
            prescription.setFollowUpDuration(null);
            prescription.setFollowUpUnit(null);
            prescription.setFollowUpDate(null);
        } else {
            prescription.setFollowUpDuration(request.getFollowUpDuration());
            prescription.setFollowUpUnit(request.getFollowUpUnit());

            // Compute followUpDate if null and duration+unit present
            LocalDate followUpDate = request.getFollowUpDate();
            if (followUpDate == null && request.getFollowUpDuration() != null && request.getFollowUpUnit() != null) {
                followUpDate = computeFollowUpDate(
                        appointment.getAppointmentDate(),
                        request.getFollowUpDuration(),
                        request.getFollowUpUnit()
                );
            }
            prescription.setFollowUpDate(followUpDate);
        }

        // Persist header first to ensure we have prescriptionId
        Prescription savedHeader = prescriptionRepository.saveAndFlush(prescription);

        // 2) Medications: delete old then insert new
        medicationRepository.deleteByPrescription_Id(savedHeader.getId());
        entityManager.flush();
        entityManager.clear();

        List<MedicationRequest> meds = request.getMedications();
        if (meds != null && !meds.isEmpty()) {
            List<PrescriptionMedication> entities = new ArrayList<>(meds.size());
            for (MedicationRequest m : meds) {
                PrescriptionMedication pm = PrescriptionMedication.builder()
                        .prescription(savedHeader)
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
                        .build();
                entities.add(pm);
            }
            medicationRepository.saveAll(entities);
        }

        // 3) Tests: reuse your existing PrescribedTestService (already does delete + insert)
        List<PrescribedTestDTO> tests = request.getTests();
        if (tests != null) {
            prescribedTestService.saveOrUpdateTests(appointmentId, tests);
        }

        // savedHeader.getSavedAt() is LocalDateTime; output with 'Z' format (UTC-like display)
        String savedAtIso = savedHeader.getSavedAt() == null
                ? null
                : savedHeader.getSavedAt().atOffset(ZoneOffset.UTC).format(ISO_TS);

        log.info("Prescription saved: appointmentId={}, prescriptionId={}", appointmentId, savedHeader.getId());

        return PrescriptionSaveResponse.builder()
                .prescriptionId(savedHeader.getId())
                .appointmentId(appointmentId)
                .savedAt(savedAtIso)
                .build();
    }

    private LocalDate computeFollowUpDate(LocalDate baseDate, int duration, String unit) {
        if (baseDate == null) baseDate = LocalDate.now();

        String u = unit.trim().toUpperCase();
        return switch (u) {
            case "DAY", "DAYS" -> baseDate.plusDays(duration);
            case "WEEK", "WEEKS" -> baseDate.plusWeeks(duration);
            case "MONTH", "MONTHS" -> baseDate.plusMonths(duration);
            default -> baseDate.plusDays(duration); // fallback
        };
    }

    @Override
    public PrescriptionFetchResponse getByAppointmentId(Long appointmentId) {

        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found with ID: " + appointmentId));

        // VisitSummary is needed to fetch tests; also used in save flow
        VisitSummary visitSummary = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("VisitSummary not found for appointmentId: " + appointmentId));

        Prescription prescription = prescriptionRepository.findByAppointment_Id(appointmentId).orElse(null);

        List<MedicationResponse> meds = Collections.emptyList();
        if (prescription != null) {
            List<PrescriptionMedication> medEntities = medicationRepository.findByPrescription_Id(prescription.getId());
            meds = medEntities.stream().map(this::mapMedication).collect(Collectors.toList());
        }

        // tests already exist as PrescribedTest linked to VisitSummary
        List<PrescribedTestDTO> tests = prescribedTestService.getTestsByAppointmentId(appointmentId);

        // If prescription doesn't exist yet, return defaults but still return tests (if any)
        if (prescription == null) {
            return PrescriptionFetchResponse.builder()
                    .language("English")
                    .followUpEnabled(false)
                    .followUpDuration(null)
                    .followUpUnit(null)
                    .followUpDate(null)
                    .sendFollowUpEmail(false)
                    .adviceText("")
                    .medications(meds)
                    .tests(tests)
                    .build();
        }

        return PrescriptionFetchResponse.builder()
                .language(prescription.getLanguage())
                .followUpEnabled(Boolean.TRUE.equals(prescription.getFollowUpEnabled()))
                .followUpDuration(prescription.getFollowUpDuration())
                .followUpUnit(prescription.getFollowUpUnit())
                .followUpDate(prescription.getFollowUpDate())
                .sendFollowUpEmail(Boolean.TRUE.equals(prescription.getSendFollowUpEmail()))
                .adviceText(prescription.getAdviceText())
                .medications(meds)
                .tests(tests)
                .build();
    }

    private MedicationResponse mapMedication(PrescriptionMedication m) {
        return MedicationResponse.builder()
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
                .build();
    }
}
