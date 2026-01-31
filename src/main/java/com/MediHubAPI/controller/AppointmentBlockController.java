package com.MediHubAPI.controller;

import com.MediHubAPI.dto.appointments.AppointmentBlockRequest;
import com.MediHubAPI.dto.appointments.AppointmentBlockResponse;
import com.MediHubAPI.dto.appointments.AppointmentShiftRequest;
import com.MediHubAPI.dto.appointments.AppointmentShiftResponse;
import com.MediHubAPI.dto.appointments.AppointmentUnblockRequest;
import com.MediHubAPI.dto.appointments.AppointmentUnblockResponse;
import com.MediHubAPI.service.api.AppointmentBlockApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentBlockController {

    private final AppointmentBlockApiService appointmentBlockApiService;

    @PostMapping("/blocks")
    public ResponseEntity<AppointmentBlockResponse> blockSlots(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AppointmentBlockRequest request) {
        AppointmentBlockResponse response = appointmentBlockApiService.blockSlots(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/blocks/unblock")
    public ResponseEntity<AppointmentUnblockResponse> unblockSlots(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AppointmentUnblockRequest request) {
        AppointmentUnblockResponse response = appointmentBlockApiService.unblockSlots(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/shift")
    public ResponseEntity<AppointmentShiftResponse> shiftAppointments(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AppointmentShiftRequest request) {
        AppointmentShiftResponse response = appointmentBlockApiService.shiftAppointments(idempotencyKey, request);
        return ResponseEntity.ok(response);
    }
}
