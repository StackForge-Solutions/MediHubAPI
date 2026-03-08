package com.MediHubAPI.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.MediHubAPI.dto.diagnosis.CreateDiagnosisRequest;
import com.MediHubAPI.dto.diagnosis.DiagnosisRowResponse;
import com.MediHubAPI.dto.diagnosis.UpdateDiagnosisByIdRequest;
import com.MediHubAPI.dto.diagnosis.UpdateDiagnosisRequest;
import com.MediHubAPI.exception.diagnosis.DiagnosisAppointmentNotFoundException;
import com.MediHubAPI.exception.diagnosis.DiagnosisValidationException;
import com.MediHubAPI.exception.diagnosis.DuplicateDiagnosisException;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.Diagnosis;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.repository.DiagnosisRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;

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
        validateCreateRequest(request);
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
            lockVisitSummary(visitSummary.getId());
            ensureNoOtherPrimary(visitSummary.getId(), null);
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
        validateUpdateRequest(request);
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
            lockVisitSummary(visitSummary.getId());
            ensureNoOtherPrimary(visitSummary.getId(), diagnosis.getId());
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

    @Transactional
    public DiagnosisRowResponse updateDiagnosisById(Long appointmentId, Long diagnosisId,
            UpdateDiagnosisByIdRequest request) {
        validateAppointmentId(appointmentId);
        validateDiagnosisId(diagnosisId);
        validateUpdateByIdRequest(request);
        validateSinceYear(request.getSinceYear());

        if (!appointmentRepository.existsById(appointmentId)) {
            throw new DiagnosisAppointmentNotFoundException(appointmentId);
        }

        Diagnosis diagnosis = diagnosisRepository.findById(diagnosisId)
                .orElseThrow(() -> validation("DIAGNOSIS_NOT_FOUND", "Diagnosis not found",
                        Map.of("diagnosisId", "Diagnosis not found")));

        VisitSummary visitSummary = diagnosis.getVisitSummary();
        if (visitSummary == null) {
            throw validation("DIAGNOSIS_VISIT_SUMMARY_MISSING", "Visit summary is missing for this diagnosis",
                    Map.of("diagnosisId", "Visit summary is missing"));
        }

        if (visitSummary.getAppointment() == null || !appointmentId.equals(visitSummary.getAppointment().getId())) {
            throw validation("DIAGNOSIS_APPOINTMENT_MISMATCH",
                    "Diagnosis does not belong to appointmentId=" + appointmentId,
                    Map.of("appointmentId", "Diagnosis does not belong to the specified appointment"));
        }

        String normalizedSource = normalizeRequired(request.getSource(), "source");
        String normalizedName = normalizeRequired(request.getName(), "name");
        String normalizedComments = normalizeOptional(request.getComments());

        if (diagnosisRepository.existsByVisitSummary_IdAndSourceIgnoreCaseAndNameIgnoreCaseAndIdNot(
                visitSummary.getId(), normalizedSource, normalizedName, diagnosis.getId())) {
            throw new DuplicateDiagnosisException();
        }

        if (Boolean.TRUE.equals(request.getPrimary())) {
            lockVisitSummary(visitSummary.getId());
            ensureNoOtherPrimary(visitSummary.getId(), diagnosis.getId());
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

        Diagnosis updated = diagnosisRepository.save(diagnosis);

        return new DiagnosisRowResponse(
                updated.getName(),
                formatSinceLabel(updated.getYears(), updated.getMonths(), updated.getDays(), true),
                toSinceDate(updated.getSinceYear()),
                updated.getChronic(),
                updated.getPrimaryDiagnosis(),
                updated.getComments()
        );
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
            throw validation("DIAGNOSIS_APPOINTMENT_ID_INVALID", "appointmentId must be a positive integer",
                    Map.of("appointmentId", "appointmentId must be a positive integer"));
        }
    }

    private void validateDiagnosisId(Long diagnosisId) {
        if (diagnosisId == null || diagnosisId <= 0) {
            throw validation("DIAGNOSIS_ID_INVALID", "diagnosisId must be a positive integer",
                    Map.of("diagnosisId", "diagnosisId must be a positive integer"));
        }
    }

    private void validateCreateRequest(CreateDiagnosisRequest request) {
        if (request == null) {
            throw validation("DIAGNOSIS_PAYLOAD_REQUIRED", "Request body is required",
                    Map.of("payload", "Request body is required"));
        }
        requireText(request.getSource(), "source");
        requireText(request.getName(), "name");
    }

    private void validateUpdateRequest(UpdateDiagnosisRequest request) {
        if (request == null) {
            throw validation("DIAGNOSIS_PAYLOAD_REQUIRED", "Request body is required",
                    Map.of("payload", "Request body is required"));
        }
        requireText(request.getSource(), "source");
        requireText(request.getCurrentName(), "currentName");
        requireText(request.getName(), "name");
    }

    private void validateUpdateByIdRequest(UpdateDiagnosisByIdRequest request) {
        if (request == null) {
            throw validation("DIAGNOSIS_PAYLOAD_REQUIRED", "Request body is required",
                    Map.of("payload", "Request body is required"));
        }
        requireText(request.getSource(), "source");
        requireText(request.getName(), "name");
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw validation("DIAGNOSIS_FIELD_REQUIRED", field + " is required",
                    Map.of(field, field + " is required"));
        }
    }

    private void validateSinceYear(Integer sinceYear) {
        if (sinceYear == null) {
            return;
        }

        int currentYear = Year.now(ZoneOffset.UTC).getValue();
        if (sinceYear > currentYear) {
            throw validation("DIAGNOSIS_SINCE_YEAR_FUTURE", "sinceYear cannot be in the future",
                    Map.of("sinceYear", "sinceYear cannot be in the future"));
        }
        if (sinceYear < 1800) {
            throw validation("DIAGNOSIS_SINCE_YEAR_INVALID", "sinceYear is too far in the past",
                    Map.of("sinceYear", "sinceYear must be greater than 1800"));
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        requireText(value, fieldName);
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
                "DIAGNOSIS_NOT_FOUND",
                "VALIDATION_ERROR",
                "Diagnosis not found",
                Map.of("currentName", "Diagnosis not found"),
                List.of("Diagnosis not found")
        );
    }

    private DiagnosisValidationException validation(String errorCode, String message, Map<String, String> fields) {
        return new DiagnosisValidationException(
                errorCode,
                "VALIDATION_ERROR",
                message,
                fields,
                List.of(message)
        );
    }

    private void lockVisitSummary(Long visitSummaryId) {
        visitSummaryRepository.findByIdForUpdate(visitSummaryId)
                .orElseThrow(() -> validation("DIAGNOSIS_VISIT_SUMMARY_NOT_FOUND",
                        "Visit summary not found",
                        Map.of("visitSummaryId", "Visit summary not found")));
    }

    private void ensureNoOtherPrimary(Long visitSummaryId, Long currentDiagnosisId) {
        diagnosisRepository.findFirstByVisitSummary_IdAndPrimaryDiagnosisTrue(visitSummaryId)
                .filter(existing -> !existing.getId().equals(currentDiagnosisId))
                .ifPresent(existing -> {
                    throw new DiagnosisValidationException(
                            "DIAGNOSIS_PRIMARY_EXISTS",
                            "VALIDATION_ERROR",
                            "Another primary diagnosis already exists",
                            Map.of("primary", "Primary diagnosis already set for this visit"),
                            List.of("Primary diagnosis already exists")
                    );
                });
    }
}
