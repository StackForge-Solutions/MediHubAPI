package com.MediHubAPI.repository;

import com.MediHubAPI.dto.VisitRowDTO;
import com.MediHubAPI.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface AppointmentQueryRepository extends JpaRepository<Appointment, Long> {

    @Query("""
    SELECT new com.MediHubAPI.dto.VisitRowDTO(
        a.id,
        a.type,
        d.id,
        d.name,
        p.id,
        p.name,
        u.mobileNumber,
        a.appointmentDate,
        s.slotTime,
        COALESCE(a.createdBy, s.createdBy)
    )
    FROM Appointment a
    JOIN a.doctor d
    JOIN a.patient p
    JOIN p.user u
    JOIN a.slot s
    WHERE a.appointmentDate BETWEEN :start AND :end
      AND (:doctorId IS NULL OR d.id = :doctorId)
      AND (:patientId IS NULL OR p.id = :patientId)
""")
    Page<VisitRowDTO> findVisitRows(LocalDate start,
                                    LocalDate end,
                                    Long doctorId,
                                    Long patientId,
                                    Pageable pageable);

}
