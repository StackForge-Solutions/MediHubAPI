package com.MediHubAPI.service;

import com.MediHubAPI.dto.AppointmentBookingDto;
import com.MediHubAPI.dto.AppointmentResponseDto;
import com.MediHubAPI.dto.DoctorScheduleDto;
import com.MediHubAPI.dto.api.AppointmentApiDto;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.enums.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    AppointmentResponseDto bookAppointment(AppointmentBookingDto dto);

    AppointmentResponseDto bookAppointment(AppointmentBookingDto dto, Appointment rescheduledFrom);

    List<AppointmentResponseDto> getAppointmentsForDoctor(Long doctorId, LocalDate date);

    void cancelAppointment(Long appointmentId);

    AppointmentResponseDto reschedule(Long id, AppointmentBookingDto dto);

    Page<AppointmentResponseDto> getAppointmentsForPatient(Long patientId, Pageable pageable);

    Page<AppointmentResponseDto> getAppointmentsWithFilters(LocalDate date, String doctorName, AppointmentStatus status, String range, Pageable pageable);

    List<AppointmentApiDto> getAppointmentsForApi(LocalDate date, Long doctorId, Long departmentId, AppointmentStatus status);

    void markAsArrived(Long appointmentId);

    Page<DoctorScheduleDto> getDoctorSchedulesStructured(LocalDate date, String doctorName, String specialization, Pageable pageable);
}
