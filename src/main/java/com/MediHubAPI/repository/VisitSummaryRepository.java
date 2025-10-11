package com.MediHubAPI.repository;


import com.MediHubAPI.model.VisitSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

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
}
