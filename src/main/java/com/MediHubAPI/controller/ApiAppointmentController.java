package com.MediHubAPI.controller;

import com.MediHubAPI.dto.api.AppointmentConfirmRequest;
import com.MediHubAPI.dto.api.AppointmentConfirmResponse;
import com.MediHubAPI.dto.api.AppointmentListResponse;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.enums.AppointmentStatus;
import com.MediHubAPI.service.api.ApiAppointmentService;
import com.MediHubAPI.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Slf4j
public class ApiAppointmentController {

    private final AppointmentService appointmentService;
    private final ApiAppointmentService apiAppointmentService;

    @GetMapping
    public ResponseEntity<AppointmentListResponse> getAppointments(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String status) {

        if (date == null) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "date is required in YYYY-MM-DD");
        }

        validatePositive(doctorId, "doctorId");
        validatePositive(departmentId, "departmentId");

        AppointmentStatus mappedStatus = mapStatus(status);

        log.info("GET /api/appointments date={} doctorId={} departmentId={} status={}", date, doctorId, departmentId, status);

        AppointmentListResponse response = AppointmentListResponse.builder()
                .status(200)
                .message("ok")
                .data(appointmentService.getAppointmentsForApi(date, doctorId, departmentId, mappedStatus))
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<AppointmentConfirmResponse> confirmBooking(
            @Valid @RequestBody AppointmentConfirmRequest request) {
        AppointmentConfirmResponse response = apiAppointmentService.confirmBooking(request);
        return ResponseEntity.ok(response);
    }

    private void validatePositive(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", fieldName + " must be a positive integer");
        }
    }

    private AppointmentStatus mapStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "CONFIRMED" -> AppointmentStatus.BOOKED;
            case "ARRIVED" -> AppointmentStatus.ARRIVED;
            case "COMPLETED" -> AppointmentStatus.COMPLETED;
            case "NO_SHOW" -> AppointmentStatus.NO_SHOW;
            case "CANCELLED" -> AppointmentStatus.CANCELLED;
            default -> throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                    "status must be one of CONFIRMED, ARRIVED, COMPLETED, NO_SHOW, CANCELLED");
        };
    }
}
