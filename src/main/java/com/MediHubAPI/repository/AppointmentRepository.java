package com.MediHubAPI.repository;


import com.MediHubAPI.dto.PatientDetailsDto;
import com.MediHubAPI.dto.PatientResponseDto;
import com.MediHubAPI.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;


import com.MediHubAPI.model.User;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalTime;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>, JpaSpecificationExecutor<Appointment> {
    boolean existsByPatientAndAppointmentDateAndSlotTime(User patient, LocalDate date, LocalTime time);

    boolean existsByDoctorAndAppointmentDateAndSlotTime(User doctor, LocalDate date, LocalTime time);


    List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, LocalDate date);

    List<Appointment> findByPatientIdOrderByAppointmentDateDesc(Long patientId);

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    @Query("""
                SELECT a FROM Appointment a
                WHERE a.patient.id = :patientId
                AND (:cursor IS NULL OR a.appointmentDate < :cursorDate OR 
                     (a.appointmentDate = :cursorDate AND a.id < :cursorId))
                ORDER BY a.appointmentDate DESC, a.id DESC
            """)
    List<Appointment> findAppointmentsCursorBased(
            @Param("patientId") Long patientId,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    // Get all patients from appointments
    @Query("SELECT a.patient FROM Appointment a")
    List<User> findAllPatients();

    // By single date
    @Query("""
        SELECT new com.MediHubAPI.dto.PatientDetailsDto(
            p.id,
            CONCAT(p.firstName, ' ', p.lastName),
            p.hospitalId,
            p.mobileNumber,
            a.type,
            a.appointmentDate,
            a.slotTime,
            CONCAT(d.firstName, ' ', d.lastName),
            a.slot.createdBy
        )
        FROM Appointment a
        JOIN a.patient p
        JOIN a.doctor d
        WHERE a.appointmentDate = :date
        ORDER BY a.slotTime
    """)
    List<PatientDetailsDto> findPatientsByDate(@Param("date") LocalDate date);

    // By date range (week/month/next/previous)
    @Query("""
        SELECT new com.MediHubAPI.dto.PatientDetailsDto(
            p.id,
            CONCAT(p.firstName, ' ', p.lastName),
            p.hospitalId,
            p.mobileNumber,
            a.type,
            a.appointmentDate,
            a.slotTime,
            CONCAT(d.firstName, ' ', d.lastName),
            a.slot.createdBy
        )
        FROM Appointment a
        JOIN a.patient p
        JOIN a.doctor d
        WHERE a.appointmentDate BETWEEN :start AND :end
        ORDER BY a.appointmentDate, a.slotTime
    """)
    List<PatientDetailsDto> findPatientsByDateRange(@Param("start") LocalDate start, @Param("end") LocalDate end);


}
