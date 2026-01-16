package com.MediHubAPI.repository;

import com.MediHubAPI.dto.VisitRowDTO;
import com.MediHubAPI.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;


import java.time.LocalDate;


public interface AppointmentQueryRepository extends JpaRepository<Appointment, Long> {

//    @Query(value = """
//                SELECT new com.MediHubAPI.dto.VisitRowDTO(
//                    a.id,
//                    a.doctor.id,
//                    CONCAT(a.doctor.firstName, ' ', a.doctor.lastName),
//                    a.patient.id,
//                    CONCAT(a.patient.firstName, ' ', a.patient.lastName),
//                    a.patient.mobileNumber,
//                    a.appointmentDate,
//                    a.slotTime,
//                    a.type,
//                    a.createdBy,
//                    s.startTime,
//                    s.endTime,
//                    i.id,
//                    CASE WHEN i.balanceDue = 0 THEN true ELSE false END,
//                    i.status
//
//                )
//                FROM Appointment a
//                LEFT JOIN a.slot s
//                LEFT JOIN Invoice i
//                    ON i.appointmentId = a.id
//                   AND i.id = (
//                        SELECT MAX(i2.id)
//                        FROM Invoice i2
//                        WHERE i2.appointmentId = a.id
//                          AND i2.status <> com.MediHubAPI.model.billing.Invoice.Status.VOID
//                   )
//                WHERE a.appointmentDate BETWEEN :start AND :end
//                  AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
//                  AND (:patientId IS NULL OR a.patient.id = :patientId)
//            """, countQuery = """
//                SELECT COUNT(a)
//                FROM Appointment a
//                WHERE a.appointmentDate BETWEEN :start AND :end
//                  AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
//                  AND (:patientId IS NULL OR a.patient.id = :patientId)
//            """)
//    Page<VisitRowDTO> findVisitRowsV2(@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("doctorId") Long doctorId, @Param("patientId") Long patientId, Pageable pageable);

    @Query(value = """
    SELECT new com.MediHubAPI.dto.VisitRowDTO(
        a.id,
        a.doctor.id,
        CONCAT(a.doctor.firstName, ' ', a.doctor.lastName),
        a.patient.id,
        CONCAT(a.patient.firstName, ' ', a.patient.lastName),
        a.patient.mobileNumber,
        a.appointmentDate,
        a.slotTime,
        a.type,
        a.createdBy,
        s.startTime,
        s.endTime,
        i.id,
        CASE WHEN i.balanceDue = 0 THEN true ELSE false END,
        i.status
    )
    FROM Appointment a
    LEFT JOIN a.slot s
    LEFT JOIN Invoice i
        ON i.appointmentId = a.id
       AND i.id = (
            SELECT MAX(i2.id)
            FROM Invoice i2
            WHERE i2.appointmentId = a.id
              AND i2.status <> com.MediHubAPI.model.billing.Invoice.Status.VOID
       )
    WHERE a.appointmentDate BETWEEN :start AND :end
      AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
      AND (:patientId IS NULL OR a.patient.id = :patientId)
      AND (
          :hasLabTests = false
          OR (
              :hasLabTests = true
              AND EXISTS (
                  SELECT 1
                  FROM PrescribedTest pt
                  JOIN pt.visitSummary vs
                  WHERE vs.appointment.id = a.id
              )
          )
      )
""",
            countQuery = """
    SELECT COUNT(a)
    FROM Appointment a
    WHERE a.appointmentDate BETWEEN :start AND :end
      AND (:doctorId IS NULL OR a.doctor.id = :doctorId)
      AND (:patientId IS NULL OR a.patient.id = :patientId)
      AND (
          :hasLabTests = false
          OR (
              :hasLabTests = true
              AND EXISTS (
                  SELECT 1
                  FROM PrescribedTest pt
                  JOIN pt.visitSummary vs
                  WHERE vs.appointment.id = a.id
              )
          )
      )
""")
    Page<VisitRowDTO> findVisitRows(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("doctorId") Long doctorId,
            @Param("patientId") Long patientId,
            @Param("hasLabTests") Boolean hasLabTests,
            Pageable pageable
    );




}
