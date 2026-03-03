package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.AppointmentBookingDto;
import com.MediHubAPI.dto.AppointmentResponseDto;
import com.MediHubAPI.dto.DoctorScheduleDto;
import com.MediHubAPI.model.enums.AppointmentStatus;
import com.MediHubAPI.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> bookAppointment(
            @Valid @RequestBody AppointmentBookingDto bookingDto) {
        log.info("📅 Booking appointment for doctorId={} and patientId={}", bookingDto.getDoctorId(), bookingDto.getPatientId());
        AppointmentResponseDto booked = appointmentService.bookAppointment(bookingDto);
        return ResponseEntity.status(201).body(ApiResponse.created(booked, "/appointments/book", "Appointment booked successfully"));
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<ApiResponse<List<AppointmentResponseDto>>> getAppointmentsByDoctor(
            @PathVariable Long doctorId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("📅 Fetching appointments for doctorId={} on date={}", doctorId, date);
        List<AppointmentResponseDto> list = appointmentService.getAppointmentsForDoctor(doctorId, date);
        return ResponseEntity.ok(ApiResponse.success(list, "/appointments/" + doctorId, "Appointments fetched"));
    }
    @GetMapping("/doctor-schedules/paged")
    public ResponseEntity<ApiResponse<Page<DoctorScheduleDto>>> getDoctorSchedulesPaged(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) String specialization,
            Pageable pageable
    ) {
        Page<DoctorScheduleDto> page = appointmentService.getDoctorSchedulesStructured(date, doctorName, specialization, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "/doctor-schedules/paged", "Doctor schedules paginated"));
    }


    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<AppointmentResponseDto>>> getPatientAppointments(
            @PathVariable Long patientId,
            Pageable pageable) {
        log.info("📄 Fetching paginated appointments for patientId={} with pageable={}", patientId, pageable);
        Page<AppointmentResponseDto> page = appointmentService.getAppointmentsForPatient(patientId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page, "/appointments/patient/" + patientId, "Appointments fetched"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancelAppointment(@PathVariable Long id) {
        log.info("🗑️ Cancelling appointment with ID={}", id);
        appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled", "/appointments/" + id));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<ApiResponse<AppointmentResponseDto>> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentBookingDto dto) {
        log.info("♻️ Rescheduling appointment ID={} to date={} and time={}", id, dto.getAppointmentDate(), dto.getSlotTime());
        AppointmentResponseDto rescheduled = appointmentService.reschedule(id, dto);
        return ResponseEntity.ok(ApiResponse.success(rescheduled, "/appointments/" + id + "/reschedule", "Appointment rescheduled"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AppointmentResponseDto>>> getAppointmentsByDateWithFilters(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) String range,// e.g. TODAY, WEEK, MONTH
            Pageable pageable) {

        log.info("📄 Fetching appointments by date={}, range={}, doctorName={}, status={}", date, range, doctorName, status);
        Page<AppointmentResponseDto> appointments = appointmentService.getAppointmentsWithFilters(date, doctorName, status, range, pageable);
        return ResponseEntity.ok(ApiResponse.success(appointments, "/appointments", "Appointments fetched with filters"));
    }

    @PutMapping("/{id}/arrive")
    public ResponseEntity<ApiResponse<Void>> markAsArrived(@PathVariable Long id) {
        log.info(" Marking appointment ID={} as ARRIVED", id);
        appointmentService.markAsArrived(id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as arrived", "/appointments/" + id + "/arrive"));
    }

}
