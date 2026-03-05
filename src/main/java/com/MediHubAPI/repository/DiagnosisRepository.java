package com.MediHubAPI.repository;

import com.MediHubAPI.model.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    boolean existsByVisitSummary_IdAndSourceIgnoreCaseAndNameIgnoreCase(Long visitSummaryId, String source, String name);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Diagnosis d
           SET d.primaryDiagnosis = false
           WHERE d.visitSummary.id = :visitSummaryId
             AND d.primaryDiagnosis = true
           """)
    int clearPrimaryForVisitSummary(@Param("visitSummaryId") Long visitSummaryId);

    List<Diagnosis> findByVisitSummary_Appointment_IdOrderByIdAsc(Long appointmentId);
}
