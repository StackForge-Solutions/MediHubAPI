package com.MediHubAPI.repository;

import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.repository.projection.EmrQueueRowProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EmrQueueRepository extends JpaRepository<Appointment, Long> {

    @Query(value = """
        SELECT
          ROW_NUMBER() OVER (ORDER BY i.token_no ASC, COALESCE(a.created_at, i.created_at) ASC) AS rowId,
          CAST(i.token_no AS CHAR) AS tokenNo,

          a.doctor_id AS doctorId,
          a.id AS appointmentId,

                  
                  
                    a.slot_time AS slotTime,              
                    a.appointment_date AS visitDate,      

          a.patient_id AS patientId,
          CONCAT(pu.first_name, ' ', pu.last_name) AS patientName,

          CONCAT(
            CASE COALESCE(pat.sex, pu.sex)
              WHEN 'MALE' THEN 'Male'
              WHEN 'FEMALE' THEN 'Female'
              ELSE 'Other'
            END,
            ' | (',
            CASE
              WHEN pu.dob IS NULL THEN 'NA'
              ELSE CONCAT(TIMESTAMPDIFF(YEAR, pu.dob, :date), ' Y')
            END,
            ')'
          ) AS ageSexLabel,

          CONCAT(du.first_name, ' ', du.last_name) AS doctorName,

          COALESCE(a.created_at, i.created_at, TIMESTAMP(a.appointment_date, a.slot_time)) AS createdAt,

          '' AS alerts,

          CASE a.status
            WHEN 'BOOKED' THEN 'Reserved'
            WHEN 'ARRIVED' THEN 'Waiting'
            WHEN 'COMPLETED' THEN 'Completed'
            WHEN 'CANCELLED' THEN 'Cancelled'
            WHEN 'NO_SHOW' THEN 'No Show'
            ELSE a.status
          END AS status,

          IF(
            UPPER(COALESCE(i.queue, '')) LIKE '%INSUR%'
            OR UPPER(COALESCE(i.notes, '')) LIKE '%INSUR%',
            TRUE,
            FALSE
          ) AS hasInsurance,

          IF(
            (pat.referrer_name IS NOT NULL AND pat.referrer_name <> '')
            OR (pat.referrer_number IS NOT NULL AND pat.referrer_number <> '')
            OR (pat.referrer_type IS NOT NULL AND pat.referrer_type <> ''),
            TRUE,
            FALSE
          ) AS hasReferrer

        FROM appointments a
        JOIN invoices i
          ON i.appointment_id = a.id
         AND (i.status = 'PAID' OR i.balance_due = 0)

        JOIN users du ON du.id = a.doctor_id
        JOIN users pu ON pu.id = a.patient_id
        LEFT JOIN patients pat ON pat.user_id = a.patient_id

        WHERE a.appointment_date = :date
          AND a.status IN ('BOOKED', 'ARRIVED')
          AND (:doctorId IS NULL OR a.doctor_id = :doctorId)

        ORDER BY i.token_no ASC, COALESCE(a.created_at, i.created_at) ASC
        """, nativeQuery = true)
    List<EmrQueueRowProjection> fetchEmrQueue(
            @Param("date") LocalDate date,
            @Param("doctorId") Long doctorId
    );
}
