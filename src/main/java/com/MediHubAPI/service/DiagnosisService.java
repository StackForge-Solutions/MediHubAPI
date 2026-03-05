package com.MediHubAPI.service;

import com.MediHubAPI.dto.diagnosis.CreateDiagnosisRequest;
import com.MediHubAPI.dto.diagnosis.DiagnosisRowResponse;
import com.MediHubAPI.dto.diagnosis.UpdateDiagnosisRequest;
import com.MediHubAPI.exception.diagnosis.DiagnosisAppointmentNotFoundException;
import com.MediHubAPI.exception.diagnosis.DiagnosisInvalidInputException;
import com.MediHubAPI.exception.diagnosis.DiagnosisValidationException;
import com.MediHubAPI.exception.diagnosis.DuplicateDiagnosisException;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.Diagnosis;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.DiagnosisRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosisService {

    private final AppointmentRepository appointmentRepository;
    private final VisitSummaryRepository visitSummaryRepository;
    private final DiagnosisRepository diagnosisRepository;

    @Transactional
    public DiagnosisRowResponse createDiagnosis(Long appointmentId, CreateDiagnosisRequest request) {
        validateAppointmentId(appointmentId);
        validateSinceYear(request.getSinceYear());

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new DiagnosisAppointmentNotFoundException(appointmentId));

        VisitSummary visitSummary = resolveVisitSummary(appointmentId, appointment);

        String normalizedSource = normalizeRequired(request.getSource(), "source");
        String normalizedName = normalizeRequired(request.getName(), "name");
        String normalizedComments = normalizeOptional(request.getComments());

        if (diagnosisRepository.existsByVisitSummary_IdAndSourceIgnoreCaseAndNameIgnoreCase(
                visitSummary.getId(), normalizedSource, normalizedName)) {
            throw new DuplicateDiagnosisException();
        }

        if (Boolean.TRUE.equals(request.getPrimary())) {
            diagnosisRepository.clearPrimaryForVisitSummary(visitSummary.getId());
        }

        Diagnosis saved;
        try {
            saved = diagnosisRepository.save(Diagnosis.builder()
                    .visitSummary(visitSummary)
                    .source(normalizedSource)
                    .name(normalizedName)
                    .years(request.getYears())
                    .months(request.getMonths())
                    .days(request.getDays())
                    .sinceYear(request.getSinceYear())
                    .chronic(request.getChronic())
                    .primaryDiagnosis(request.getPrimary())
                    .comments(normalizedComments)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateDiagnosisException();
        }

        return new DiagnosisRowResponse(
                saved.getName(),
                formatSinceLabel(saved.getYears(), saved.getMonths(), saved.getDays(), true),
                toSinceDate(saved.getSinceYear()),
                saved.getChronic(),
                saved.getPrimaryDiagnosis(),
                saved.getComments()
        );
    }

    @Transactional
    public DiagnosisRowResponse updateDiagnosis(Long appointmentId, UpdateDiagnosisRequest request) {
        validateAppointmentId(appointmentId);
        validateSinceYear(request.getSinceYear());

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new DiagnosisAppointmentNotFoundException(appointmentId));

        VisitSummary visitSummary = resolveVisitSummary(appointmentId, appointment);

        String normalizedSource = normalizeRequired(request.getSource(), "source");
        String normalizedCurrentName = normalizeRequired(request.getCurrentName(), "currentName");
        String normalizedName = normalizeRequired(request.getName(), "name");
        String normalizedComments = normalizeOptional(request.getComments());

        Diagnosis diagnosis = diagnosisRepository.findByVisitSummary_IdAndSourceIgnoreCaseAndNameIgnoreCase(
                        visitSummary.getId(), normalizedSource, normalizedCurrentName)
                .orElseThrow(this::diagnosisNotFoundValidation);

        if (diagnosisRepository.existsByVisitSummary_IdAndSourceIgnoreCaseAndNameIgnoreCaseAndIdNot(
                visitSummary.getId(), normalizedSource, normalizedName, diagnosis.getId())) {
            throw new DuplicateDiagnosisException();
        }

        if (Boolean.TRUE.equals(request.getPrimary())) {
            diagnosisRepository.clearPrimaryForVisitSummary(visitSummary.getId());
        }

        Integer effectiveSinceYear = request.getSinceYear() != null ? request.getSinceYear() : diagnosis.getSinceYear();

        diagnosis.setSource(normalizedSource);
        diagnosis.setName(normalizedName);
        diagnosis.setYears(request.getYears());
        diagnosis.setMonths(request.getMonths());
        diagnosis.setDays(request.getDays());
        diagnosis.setSinceYear(effectiveSinceYear);
        diagnosis.setChronic(request.getChronic());
        diagnosis.setPrimaryDiagnosis(request.getPrimary());
        diagnosis.setComments(normalizedComments);

        Diagnosis updated;
        try {
            updated = diagnosisRepository.save(diagnosis);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateDiagnosisException();
        }

        return new DiagnosisRowResponse(
                updated.getName(),
                formatSinceLabel(updated.getYears(), updated.getMonths(), updated.getDays(), true),
                toSinceDate(updated.getSinceYear()),
                updated.getChronic(),
                updated.getPrimaryDiagnosis(),
                updated.getComments()
        );
    }

    @Transactional(readOnly = true)
    public List<DiagnosisRowResponse> fetchDiagnoses(Long appointmentId) {
        validateAppointmentId(appointmentId);

        if (!appointmentRepository.existsById(appointmentId)) {
            throw new DiagnosisAppointmentNotFoundException(appointmentId);
        }

        return diagnosisRepository.findByVisitSummary_Appointment_IdOrderByIdAsc(appointmentId)
                .stream()
                .map(this::toDiagnosisRowResponse)
                .collect(Collectors.toList());
    }

    private VisitSummary resolveVisitSummary(Long appointmentId, Appointment appointment) {
        return visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> {
                    log.info("No VisitSummary found for appointmentId={}, creating one", appointmentId);
                    VisitSummary visitSummary = VisitSummary.builder()
                            .appointment(appointment)
                            .doctor(appointment.getDoctor())
                            .patient(appointment.getPatient())
                            .visitDate(appointment.getAppointmentDate() != null
                                    ? appointment.getAppointmentDate().toString()
                                    : null)
                            .visitTime(appointment.getSlotTime() != null
                                    ? appointment.getSlotTime().toString()
                                    : null)
                            .build();
                    return visitSummaryRepository.saveAndFlush(visitSummary);
                });
    }

    private void validateAppointmentId(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            throw new DiagnosisInvalidInputException("appointmentId must be a positive integer");
        }
    }

    private void validateSinceYear(Integer sinceYear) {
        if (sinceYear == null) {
            return;
        }

        int currentYear = Year.now(ZoneOffset.UTC).getValue();
        if (sinceYear > currentYear) {
            throw new DiagnosisInvalidInputException("sinceYear cannot be in the future");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new DiagnosisInvalidInputException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Instant toSinceDate(Integer sinceYear) {
        if (sinceYear == null) {
            return null;
        }
        return LocalDate.of(sinceYear, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private DiagnosisRowResponse toDiagnosisRowResponse(Diagnosis diagnosis) {
        return new DiagnosisRowResponse(
                diagnosis.getName(),
                formatSinceLabel(diagnosis.getYears(), diagnosis.getMonths(), diagnosis.getDays(), false),
                toSinceDate(diagnosis.getSinceYear()),
                diagnosis.getChronic(),
                diagnosis.getPrimaryDiagnosis(),
                diagnosis.getComments()
        );
    }

    private String formatSinceLabel(Integer years, Integer months, Integer days, boolean includeZeroFallback) {
        List<String> parts = new ArrayList<>();
        appendPart(parts, years, "yr", "yrs");
        appendPart(parts, months, "mth", "mths");
        appendPart(parts, days, "day", "days");

        if (parts.isEmpty()) {
            return includeZeroFallback ? "0 day" : null;
        }
        return String.join(", ", parts);
    }

    private void appendPart(List<String> parts, Integer value, String singular, String plural) {
        if (value == null || value <= 0) {
            return;
        }
        parts.add(value + " " + (value == 1 ? singular : plural));
    }

    private DiagnosisValidationException diagnosisNotFoundValidation() {
        return new DiagnosisValidationException(
                "DIAGNOSIS_002",
                "DIAGNOSIS_002",
                "Validation failed",
                Map.of("currentName", "currentName is required"),
                List.of("Diagnosis not found")
        );
    }
}
