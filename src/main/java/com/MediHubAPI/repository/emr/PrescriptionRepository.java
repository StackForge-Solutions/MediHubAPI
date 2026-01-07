package com.MediHubAPI.repository.emr;

import com.MediHubAPI.model.emr.Prescription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    Optional<Prescription> findByAppointment_Id(Long appointmentId);
    @EntityGraph(attributePaths = {
            "appointment", "appointment.doctor", "visitSummary", "medications"
    })
    @Query("""
        SELECT p
        FROM Prescription p
        JOIN p.appointment a
        WHERE a.patient.id = :patientId
        ORDER BY a.appointmentDate DESC, a.id DESC
    """)
    List<Prescription> findLatestByPatient(@Param("patientId") Long patientId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "appointment", "appointment.doctor", "visitSummary", "medications"
    })
    @Query("""
        SELECT p
        FROM Prescription p
        JOIN p.appointment a
        WHERE a.patient.id = :patientId
          AND (a.appointmentDate < :cursorDate OR (a.appointmentDate = :cursorDate AND a.id = :cursorAppointmentId))
        ORDER BY a.appointmentDate DESC, a.id DESC
    """)
    List<Prescription> findPreviousByPatientCursor(
            @Param("patientId") Long patientId,
            @Param("cursorDate") LocalDate cursorDate,
            @Param("cursorAppointmentId") Long cursorAppointmentId,
            Pageable pageable
    );
}
