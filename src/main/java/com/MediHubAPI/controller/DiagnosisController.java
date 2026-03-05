package com.MediHubAPI.controller;

import com.MediHubAPI.dto.diagnosis.CreateDiagnosisRequest;
import com.MediHubAPI.dto.diagnosis.CreateDiagnosisResponse;
import com.MediHubAPI.dto.diagnosis.FetchDiagnosesResponse;
import com.MediHubAPI.dto.diagnosis.DiagnosisRowResponse;
import com.MediHubAPI.exception.diagnosis.DiagnosisInvalidInputException;
import com.MediHubAPI.exception.diagnosis.DiagnosisValidationException;
import com.MediHubAPI.service.DiagnosisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
