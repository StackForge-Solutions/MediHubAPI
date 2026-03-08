package com.MediHubAPI.repository;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.MediHubAPI.model.VisitSummary;

public interface VisitSummaryRepository extends JpaRepository<VisitSummary, Long> {


    @Query("SELECT v FROM VisitSummary v WHERE v.appointment.id = :appointmentId")
    Optional<VisitSummary> findByAppointmentId(@Param("appointmentId") Long appointmentId);


    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VisitSummary v WHERE v.id = :id")
    Optional<VisitSummary> findByIdForUpdate(@Param("id") Long id);


}
