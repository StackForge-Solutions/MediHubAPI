package com.MediHubAPI.service.emr;

import com.MediHubAPI.dto.ChiefComplaintDTO;
import com.MediHubAPI.dto.MedicalHistoryDTO;
import com.MediHubAPI.dto.VitalsDTO;
import com.MediHubAPI.dto.emr.SummaryPrintDto;
import com.MediHubAPI.dto.diagnosis.DiagnosisRowResponse;
import com.MediHubAPI.dto.AppointmentAllergyResponse;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.User;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.service.AllergyService;
import com.MediHubAPI.service.ChiefComplaintService;
import com.MediHubAPI.service.DiagnosisService;
import com.MediHubAPI.service.MedicalHistoryService;
import com.MediHubAPI.service.VitalsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryPrintService {

    private final AppointmentRepository appointmentRepository;
    private final ChiefComplaintService chiefComplaintService;
    private final VitalsService vitalsService;
    private final AllergyService allergyService;
    private final MedicalHistoryService medicalHistoryService;
    private final DiagnosisService diagnosisService;

    public SummaryPrintDto buildSummary(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Appointment not found"));

        LocalDate date = appt.getAppointmentDate();

        List<ChiefComplaintDTO> complaints = safeList(() -> chiefComplaintService.getByAppointmentId(appointmentId));
        String complaint = complaints.isEmpty() ? null : formatComplaint(complaints.get(0));

        VitalsDTO vitalsDTO = safeValue(() -> vitalsService.getVitalsByAppointmentId(appointmentId));
        SummaryPrintDto.VitalsSummary vitals = vitalsDTO == null ? null : SummaryPrintDto.VitalsSummary.builder()
                .height(vitalsDTO.getHeight())
                .weight(vitalsDTO.getWeight())
                .bmi(vitalsDTO.getBmi())
                .build();

        AppointmentAllergyResponse allergy = safeValue(() -> allergyService.getByAppointmentId(appointmentId));
        String allergies = null;
        if (allergy != null) {
            allergies = allergy.getAllergiesText() != null ? allergy.getAllergiesText() : allergy.getAllergyTemplateName();
        }

        MedicalHistoryDTO history = safeValue(() -> medicalHistoryService.getByAppointmentId(appointmentId));
        List<String> medicalHistory = history == null ? Collections.emptyList() : flatten(history.getOtherHistories());
        List<String> organInvolvement = history == null ? Collections.emptyList() : flatten(history.getOtherHistories());
        List<String> renalHistory = history == null ? Collections.emptyList() : flatten(history.getRenalHistory());
        List<String> personalHistory = history == null ? Collections.emptyList() : flatten(history.getPersonalHistory());

        List<String> diagnosis = safeList(() -> diagnosisService.fetchDiagnoses(appointmentId))
                .stream()
                .map(DiagnosisRowResponse::getName)
                .collect(Collectors.toList());

        User doctor = appt.getDoctor();
        String doctorName = doctor != null
                ? String.join(" ", nonNull(doctor.getFirstName()), nonNull(doctor.getLastName())).trim()
                : null;

        SummaryPrintDto.DoctorSummary doctorSummary = SummaryPrintDto.DoctorSummary.builder()
                .name(doctorName)
                .degree(null)
                .regn(doctor != null ? doctor.getHospitalId() : null)
                .build();

        return SummaryPrintDto.builder()
                .date(date != null ? date.toString() : null)
                .complaint(complaint)
                .vitals(vitals)
                .allergies(allergies)
                .medicalHistory(medicalHistory)
                .organInvolvement(organInvolvement)
                .renalHistory(renalHistory)
                .personalHistory(personalHistory)
                .diagnosis(diagnosis)
                .doctor(doctorSummary)
                .build();
    }

    private List<String> flatten(Object obj) {
        if (obj == null) return Collections.emptyList();
        if (obj instanceof List<?> list) {
            return list.stream().map(String::valueOf).collect(Collectors.toList());
        }
        if (obj instanceof Map<?,?> map) {
            List<String> out = new ArrayList<>();
            map.forEach((k, v) -> out.add(k + " - " + v));
            return out;
        }
        return List.of(String.valueOf(obj));
    }

    private String formatComplaint(ChiefComplaintDTO c) {
        StringBuilder sb = new StringBuilder();
        sb.append(c.getComplaint());
        List<String> parts = new ArrayList<>();
        if (c.getYears() > 0) parts.add(c.getYears() + " years");
        if (c.getMonths() > 0) parts.add(c.getMonths() + " months");
        if (c.getWeeks() > 0) parts.add(c.getWeeks() + " weeks");
        if (c.getDays() > 0) parts.add(c.getDays() + " days");
        if (!parts.isEmpty()) {
            sb.append(" since ").append(String.join(" ", parts));
        }
        return sb.toString();
    }

    private String nonNull(String s) {
        return s == null ? "" : s;
    }

    private <T> T safeValue(SupplierWithException<T> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException ex) {
            log.info("summary-print: optional component missing: {}", ex.getMessage());
            return null;
        }
    }

    private <T> List<T> safeList(SupplierWithException<List<T>> supplier) {
        try {
            List<T> data = supplier.get();
            return data != null ? data : Collections.emptyList();
        } catch (RuntimeException ex) {
            log.info("summary-print: optional list component missing: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get();
    }
}
