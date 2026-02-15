package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.repository.projection.LabQueueRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface LabQueueRepository extends PagingAndSortingRepository<Invoice, Long> {

    @Query(value = """
        SELECT
          LPAD(COALESCE(i.token_no, 0), 3, '0')                                AS token,
          i.patient_id                                                         AS patientId,
          CONCAT(pu.first_name, ' ', pu.last_name)                             AS patientName,
          CONCAT(
            CASE COALESCE(pat.sex, pu.sex)
              WHEN 'MALE' THEN 'Male'
              WHEN 'FEMALE' THEN 'Female'
              ELSE 'Other'
            END,
            ' | (',
            CASE WHEN pu.dob IS NULL THEN 'NA' ELSE CONCAT(TIMESTAMPDIFF(YEAR, pu.dob, :date), ' Y') END,
            ')'
          )                                                                    AS ageLabel,
          pu.mobile_number                                                     AS phone,
          CONCAT(du.first_name, ' ', du.last_name)                             AS doctorName,
          pat.referrer_name                                                    AS referrerName,
          COALESCE(i.created_at, NOW())                                        AS createdAt,
          DATE_FORMAT(COALESCE(i.created_at, NOW()), '%Y-%m-%dT%H:%i:%s')      AS dateISO,
          (
            SELECT COALESCE(ii.sample_status, 'PENDING')
            FROM invoice_items ii
            WHERE ii.invoice_id = i.id
              AND (ii.item_type IS NULL OR ii.item_type = 'LAB_TEST')
            ORDER BY FIELD(COALESCE(ii.sample_status, 'PENDING'),
                           'COMPLETED','PENDING','RESERVED','NO_SHOW')
            LIMIT 1
          )                                                                    AS status,
          IF(
            UPPER(COALESCE(i.queue, '')) LIKE '%INSUR%'
            OR UPPER(COALESCE(i.notes, '')) LIKE '%INSUR%', TRUE, FALSE
          )                                                                    AS insurance,
          IF(
            (pat.referrer_name IS NOT NULL AND pat.referrer_name <> '')
            OR (pat.referrer_number IS NOT NULL AND pat.referrer_number <> '')
            OR (pat.referrer_type IS NOT NULL AND pat.referrer_type <> ''), TRUE, FALSE
          )                                                                    AS referrer,
          i.notes                                                              AS notes,
          i.room                                                               AS room
        FROM invoices i
        JOIN users pu ON pu.id = i.patient_id
        LEFT JOIN patients pat ON pat.user_id = i.patient_id
        LEFT JOIN users du ON du.id = i.doctor_id
        WHERE DATE(i.created_at) = :date
          AND i.status IN ('ISSUED','PARTIALLY_PAID','PAID')
          AND i.balance_due = 0
          AND i.appointment_id IS NOT NULL
          AND (
            :status = 'all'
            OR EXISTS (
                SELECT 1 FROM invoice_items ii2
                WHERE ii2.invoice_id = i.id
                  AND (ii2.item_type IS NULL OR ii2.item_type = 'LAB_TEST')
                  AND COALESCE(ii2.sample_status, 'PENDING') = UPPER(:status)
            )
          )
          AND (:room IS NULL OR i.room = :room)
          AND (:search IS NULL OR (
                LOWER(pu.first_name) LIKE CONCAT('%', LOWER(:search), '%') OR
                LOWER(pu.last_name) LIKE CONCAT('%', LOWER(:search), '%') OR
                LOWER(CONCAT(pu.first_name, ' ', pu.last_name)) LIKE CONCAT('%', LOWER(:search), '%') OR
                pu.mobile_number LIKE CONCAT('%', :search, '%')
          ))
        ORDER BY i.created_at DESC
        """,
        countQuery = """
        SELECT COUNT(*) FROM invoices i
        JOIN users pu ON pu.id = i.patient_id
        WHERE DATE(i.created_at) = :date
          AND i.status IN ('ISSUED','PARTIALLY_PAID','PAID')
          AND i.balance_due = 0
          AND i.appointment_id IS NOT NULL
          AND (
            :status = 'all'
            OR EXISTS (
                SELECT 1 FROM invoice_items ii2
                WHERE ii2.invoice_id = i.id
                  AND (ii2.item_type IS NULL OR ii2.item_type = 'LAB_TEST')
                  AND COALESCE(ii2.sample_status, 'PENDING') = UPPER(:status)
            )
          )
          AND (:room IS NULL OR i.room = :room)
          AND (:search IS NULL OR (
                LOWER(pu.first_name) LIKE CONCAT('%', LOWER(:search), '%') OR
                LOWER(pu.last_name) LIKE CONCAT('%', LOWER(:search), '%') OR
                LOWER(CONCAT(pu.first_name, ' ', pu.last_name)) LIKE CONCAT('%', LOWER(:search), '%') OR
                pu.mobile_number LIKE CONCAT('%', :search, '%')
          ))
        """,
        nativeQuery = true)
    Page<LabQueueRowProjection> fetchQueue(
            @Param("date") LocalDate date,
            @Param("status") String status,
            @Param("search") String search,
            @Param("room") String room,
            Pageable pageable
    );
}
