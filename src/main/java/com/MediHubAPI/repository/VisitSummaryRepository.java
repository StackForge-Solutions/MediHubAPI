package com.MediHubAPI.repository;


import com.MediHubAPI.model.VisitSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VisitSummaryRepository extends JpaRepository<VisitSummary, Long> {


    @Query("""
           SELECT v
           FROM VisitSummary v
           WHERE (:patientId IS NULL OR v.patient.id = :patientId)
             AND (:doctorId IS NULL OR v.doctor.id = :doctorId)
             AND (:appointmentId IS NULL OR v.appointment.id = :appointmentId)
           """)
    List<VisitSummary> findByFilters(@Param("patientId") Long patientId,
                                     @Param("doctorId") Long doctorId,
                                     @Param("appointmentId") Long appointmentId);

    @Query("SELECT v FROM VisitSummary v WHERE v.appointment.id = :appointmentId")
    Optional<VisitSummary> findByAppointmentId(@Param("appointmentId") Long appointmentId);


    @Query("SELECT v FROM VisitSummary v WHERE v.doctor.id = :doctorId AND v.patient.id = :patientId AND v.appointment.id = :appointmentId")
    Optional<VisitSummary> findFirstByDoctorIdAndPatientIdAndAppointmentId(Long doctorId, Long patientId, Long appointmentId);



}
