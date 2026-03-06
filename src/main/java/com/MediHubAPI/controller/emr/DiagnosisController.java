package com.MediHubAPI.controller.emr;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.MediHubAPI.dto.diagnosis.CreateDiagnosisRequest;
import com.MediHubAPI.dto.diagnosis.CreateDiagnosisResponse;
import com.MediHubAPI.dto.diagnosis.DiagnosisRowResponse;
import com.MediHubAPI.dto.diagnosis.FetchDiagnosesResponse;
import com.MediHubAPI.dto.diagnosis.UpdateDiagnosisRequest;
import com.MediHubAPI.exception.diagnosis.DiagnosisInvalidInputException;
import com.MediHubAPI.exception.diagnosis.DiagnosisValidationException;
import com.MediHubAPI.service.DiagnosisService;

@RestController
@RequestMapping("/api/diagnoses")
@RequiredArgsConstructor
@Slf4j
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping
    public ResponseEntity<CreateDiagnosisResponse> createDiagnosis(
            @RequestParam(required = false) Long appointmentId,
            @Valid @RequestBody CreateDiagnosisRequest request
    ) {
        Long resolvedAppointmentId = resolveAppointmentId(appointmentId, request.getAppointmentId());
        log.info("API call: createDiagnosis appointmentId={}, source={}", resolvedAppointmentId, request.getSource());

        DiagnosisRowResponse created = diagnosisService.createDiagnosis(resolvedAppointmentId, request);
        CreateDiagnosisResponse response = new CreateDiagnosisResponse(created, "Diagnosis created successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping
    public ResponseEntity<CreateDiagnosisResponse> updateDiagnosis(
            @RequestParam(required = false) Long appointmentId,
            @RequestBody UpdateDiagnosisRequest request
    ) {
        validateUpdateRequest(request);
        Long resolvedAppointmentId = resolveUpdateAppointmentId(appointmentId, request.getAppointmentId());
        log.info("API call: updateDiagnosis appointmentId={}, source={}, currentName={}",
                resolvedAppointmentId, request.getSource(), request.getCurrentName());

        DiagnosisRowResponse updated = diagnosisService.updateDiagnosis(resolvedAppointmentId, request);
        CreateDiagnosisResponse response = new CreateDiagnosisResponse(updated, "Diagnosis updated successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<FetchDiagnosesResponse> fetchDiagnoses(
            @RequestParam(required = false) Long appointmentId
    ) {
        Long resolvedAppointmentId = resolveFetchAppointmentId(appointmentId);
        log.info("API call: fetchDiagnoses appointmentId={}", resolvedAppointmentId);

        List<DiagnosisRowResponse> data = diagnosisService.fetchDiagnoses(resolvedAppointmentId);
        FetchDiagnosesResponse response = new FetchDiagnosesResponse(data, "Diagnoses fetched successfully");
        return ResponseEntity.ok(response);
    }

    private Long resolveAppointmentId(Long queryParamAppointmentId, Long bodyAppointmentId) {
        if (queryParamAppointmentId == null && bodyAppointmentId == null) {
            throw new DiagnosisInvalidInputException("appointmentId is required");
        }

        if (queryParamAppointmentId != null && bodyAppointmentId != null
                && !queryParamAppointmentId.equals(bodyAppointmentId)) {
            throw new DiagnosisInvalidInputException("appointmentId in query and request body must match");
        }

        return queryParamAppointmentId != null ? queryParamAppointmentId : bodyAppointmentId;
    }

    private Long resolveFetchAppointmentId(Long appointmentId) {
        if (appointmentId == null) {
            throw new DiagnosisValidationException(
                    "DIAGNOSIS_002",
                    "appointmentId is required",
                    Map.of("appointmentId", "appointmentId is required"),
                    List.of("Invalid appointment id")
            );
        }

        if (appointmentId <= 0) {
            throw new DiagnosisValidationException(
                    "DIAGNOSIS_002",
                    "appointmentId must be a positive integer",
                    Map.of("appointmentId", "appointmentId must be a positive integer"),
                    List.of("Invalid appointment id")
            );
        }

        return appointmentId;
    }

    private Long resolveUpdateAppointmentId(Long queryParamAppointmentId, Long bodyAppointmentId) {
        if (queryParamAppointmentId == null && bodyAppointmentId == null) {
            throw diagnosisValidation("appointmentId", "appointmentId is required", "Invalid appointment id");
        }

        if (queryParamAppointmentId != null && queryParamAppointmentId <= 0) {
            throw diagnosisValidation("appointmentId", "appointmentId must be a positive integer",
                    "Invalid appointment id");
        }

        if (bodyAppointmentId != null && bodyAppointmentId <= 0) {
            throw diagnosisValidation("appointmentId", "appointmentId must be a positive integer",
                    "Invalid appointment id");
        }

        if (queryParamAppointmentId != null && bodyAppointmentId != null
                && !queryParamAppointmentId.equals(bodyAppointmentId)) {
            throw diagnosisValidation("appointmentId", "appointmentId in query and request body must match",
                    "Invalid appointment id");
        }

        return queryParamAppointmentId != null ? queryParamAppointmentId : bodyAppointmentId;
    }

    private void validateUpdateRequest(UpdateDiagnosisRequest request) {
        if (request == null) {
            throw diagnosisValidation("payload", "payload is required", "Validation failed");
        }

        if (request.getCurrentName() == null || request.getCurrentName().trim().isEmpty()) {
            throw diagnosisValidation("currentName", "currentName is required", "Diagnosis not found");
        }
    }

    private DiagnosisValidationException diagnosisValidation(String field, String fieldMessage, String error) {
        return new DiagnosisValidationException(
                "DIAGNOSIS_002",
                "DIAGNOSIS_002",
                "Validation failed",
                Map.of(field, fieldMessage),
                List.of(error)
        );
    }
}
