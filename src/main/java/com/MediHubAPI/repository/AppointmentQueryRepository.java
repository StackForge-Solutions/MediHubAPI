package com.MediHubAPI.repository;

import com.MediHubAPI.dto.VisitRowDTO;
import com.MediHubAPI.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;

import java.time.LocalDate;

public interface AppointmentQueryRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            SELECT new com.MediHubAPI.dto.VisitRowDTO(
                                                                a.id,
                                                                d.id,
                                                                CONCAT(COALESCE(d.firstName, ''),\s
                                                                       CONCAT(CASE WHEN d.lastName IS NULL OR d.lastName = '' THEN '' ELSE ' ' END, COALESCE(d.lastName, ''))),
                                                                p.id,
                                                                CONCAT(COALESCE(p.firstName, ''),\s
                                                                       CONCAT(CASE WHEN p.lastName IS NULL OR p.lastName = '' THEN '' ELSE ' ' END, COALESCE(p.lastName, ''))),
                                                                p.mobileNumber,
                                                                a.appointmentDate,
                                                                a.slotTime,
                                                                a.type,
                                                                a.createdBy,
                                                                s.startTime,
                                                                s.endTime
                                                            )
                                                            FROM Appointment a
                                                            JOIN a.doctor d
                                                            JOIN a.patient p
                                                            LEFT JOIN a.slot s
                                                            WHERE a.appointmentDate BETWEEN :startDate AND :endDate
                                                              AND (:doctorId IS NULL OR d.id = :doctorId)
                                                              AND (:patientId IS NULL OR p.id = :patientId)
                                                            ORDER BY a.appointmentDate DESC, a.slotTime DESC, a.id DESC
            
            """)
    Page<VisitRowDTO> findVisitRows(LocalDate startDate,
                                    LocalDate endDate,
                                    Long doctorId,
                                    Long patientId,
                                    Pageable pageable);

}
