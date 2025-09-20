package com.MediHubAPI.controller;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.Slot;
import com.MediHubAPI.service.SlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class SlotController {

    private final SlotService slotService;

    @PutMapping("/slots/{doctorId}/shift")
    public ResponseEntity<ShiftAppointmentsResult> shiftSlots(@PathVariable Long doctorId,
                                                              @Valid @RequestBody SlotShiftRequestDto request) {
        try {
            ShiftAppointmentsResult result = slotService.shiftSlots(doctorId, request);
            return ResponseEntity.ok(result);
        } catch (HospitalAPIException e) {
            log.error("Error shifting slots for doctor {}: {}", doctorId, e.getMessage());
            throw e; // preserve status/message from domain exception
        } catch (Exception e) {
            log.error("Error shifting slots for doctor {}: {}", doctorId, e.getMessage());
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to shift slots");
        }
    }


    @PutMapping("/slots/{doctorId}/block")
    public ResponseEntity<?> blockSlots(@PathVariable Long doctorId,
                                        @Valid @RequestBody SlotBlockRequestDto request) {
        try {
            slotService.blockSlots(doctorId, request);
            return ResponseEntity.ok("Slots blocked successfully");
        } catch (Exception e) {
            log.error("Error blocking slots for doctor {}: {}", doctorId, e.getMessage());
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to block slots");
        }
    }

    @PutMapping("/slots/{doctorId}/unblock")
    public ResponseEntity<?> unblockSlots(@PathVariable Long doctorId,
                                          @Valid @RequestBody SlotUnblockRequestDto request) {
        try {
            slotService.unblockSlots(doctorId, request);
            return ResponseEntity.ok("Slots unblocked successfully");
        } catch (Exception e) {
            log.error("Error unblocking slots for doctor {}: {}", doctorId, e.getMessage());
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to unblock slots");
        }
    }

    @GetMapping("/slots/status")
    public ResponseEntity<List<SlotStatusDto>> getSlotStatus(@RequestParam Long doctorId,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ResponseEntity.ok(slotService.getSlotStatuses(doctorId, date));
        } catch (Exception e) {
            log.error("Error fetching slot status for doctor {} on {}: {}", doctorId, date, e.getMessage());
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch slot statuses");
        }
    }

//    @PostMapping("/appointments/walk-in")
//    public ResponseEntity<?> bookWalkIn(@Valid @RequestBody WalkInAppointmentDto dto) {
//        try {
//            Appointment appointment = slotService.bookWalkInSlot(dto);
//            return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
//        } catch (Exception e) {
//            log.error("Error booking walk-in for doctor {} at {}: {}", dto.getDoctorId(), dto.getTime(), e.getMessage());
//            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to book walk-in appointment");
//        }
//    }

    @GetMapping("/slots/emergency")
    public ResponseEntity<List<Slot>> getEmergencySlots(@RequestParam Long doctorId,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            return ResponseEntity.ok(slotService.getEmergencySlots(doctorId, date != null ? date : LocalDate.now()));
        } catch (Exception e) {
            log.error("Error fetching emergency slots: {}", e.getMessage());
            throw new HospitalAPIException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch emergency slots");
        }
    }
}
