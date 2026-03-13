package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.PharmacyStockTransaction;
import com.MediHubAPI.repository.projection.MedicineTransactionProjection;
import com.MediHubAPI.repository.projection.PharmacyTransactionDetailProjection;
import com.MediHubAPI.repository.projection.PharmacyTransactionRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PharmacyStockTransactionRepository extends JpaRepository<PharmacyStockTransaction, Long> {

    @Query(value = """
            SELECT
              t.id AS transactionId,
              t.transaction_time AS transactionTime,
              t.transaction_type AS transactionType,
              b.batch_no AS batchNo,
              t.qty_in AS qtyIn,
              t.qty_out AS qtyOut,
              t.balance_after AS balanceAfter,
              t.reference_type AS referenceType,
              t.reference_id AS referenceId,
              t.reference_no AS referenceNo,
              t.note AS note
            FROM pharmacy_stock_transaction t
            LEFT JOIN pharmacy_stock_batch b ON b.id = t.batch_id
            WHERE t.medicine_id = :medicineId
            ORDER BY t.transaction_time DESC, t.id DESC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM pharmacy_stock_transaction t
                    WHERE t.medicine_id = :medicineId
                    """,
            nativeQuery = true)
    Page<MedicineTransactionProjection> findByMedicineId(@Param("medicineId") Long medicineId, Pageable pageable);

    @Query(value = """
            SELECT
              t.id AS transactionId,
              t.transaction_time AS transactionTime,
              m.id AS medicineId,
              m.brand AS medicineName,
              b.id AS batchId,
              b.batch_no AS batchNo,
              v.id AS vendorId,
              v.vendor_name AS vendorName,
              t.transaction_type AS transactionType,
              t.qty_in AS qtyIn,
              t.qty_out AS qtyOut,
              t.balance_after AS balanceAfter,
              t.unit_cost AS unitCost,
              t.unit_price AS unitPrice,
              t.reference_type AS referenceType,
              t.reference_id AS referenceId,
              t.reference_no AS referenceNo,
              t.created_by AS createdBy,
              t.note AS note
            FROM pharmacy_stock_transaction t
            INNER JOIN mdm_medicines m ON m.id = t.medicine_id
            LEFT JOIN pharmacy_stock_batch b ON b.id = t.batch_id
            LEFT JOIN pharmacy_vendor v ON v.id = b.vendor_id
            WHERE (:medicineId IS NULL OR t.medicine_id = :medicineId)
              AND (:vendorId IS NULL OR v.id = :vendorId)
              AND (:transactionType IS NULL OR t.transaction_type = :transactionType)
              AND (:batchNo IS NULL OR LOWER(COALESCE(b.batch_no, '')) LIKE CONCAT('%', LOWER(:batchNo), '%'))
              AND (:referenceType IS NULL OR t.reference_type = :referenceType)
              AND (:referenceId IS NULL OR t.reference_id = :referenceId)
              AND (:fromDateTime IS NULL OR t.transaction_time >= :fromDateTime)
              AND (:toDateTimeExclusive IS NULL OR t.transaction_time < :toDateTimeExclusive)
              AND (
                    :q IS NULL
                    OR LOWER(COALESCE(t.reference_no, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(t.note, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(b.batch_no, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(m.brand, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(m.code, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(m.composition, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(v.vendor_name, '')) LIKE CONCAT('%', LOWER(:q), '%')
              )
            ORDER BY t.transaction_time DESC, t.id DESC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM pharmacy_stock_transaction t
                    INNER JOIN mdm_medicines m ON m.id = t.medicine_id
                    LEFT JOIN pharmacy_stock_batch b ON b.id = t.batch_id
                    LEFT JOIN pharmacy_vendor v ON v.id = b.vendor_id
                    WHERE (:medicineId IS NULL OR t.medicine_id = :medicineId)
                      AND (:vendorId IS NULL OR v.id = :vendorId)
                      AND (:transactionType IS NULL OR t.transaction_type = :transactionType)
                      AND (:batchNo IS NULL OR LOWER(COALESCE(b.batch_no, '')) LIKE CONCAT('%', LOWER(:batchNo), '%'))
                      AND (:referenceType IS NULL OR t.reference_type = :referenceType)
                      AND (:referenceId IS NULL OR t.reference_id = :referenceId)
                      AND (:fromDateTime IS NULL OR t.transaction_time >= :fromDateTime)
                      AND (:toDateTimeExclusive IS NULL OR t.transaction_time < :toDateTimeExclusive)
                      AND (
                            :q IS NULL
                            OR LOWER(COALESCE(t.reference_no, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(t.note, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(b.batch_no, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(m.brand, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(m.code, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(m.composition, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(v.vendor_name, '')) LIKE CONCAT('%', LOWER(:q), '%')
                      )
                    """,
            nativeQuery = true)
    Page<PharmacyTransactionRowProjection> searchTransactions(@Param("q") String q,
                                                              @Param("medicineId") Long medicineId,
                                                              @Param("vendorId") Long vendorId,
                                                              @Param("transactionType") String transactionType,
                                                              @Param("batchNo") String batchNo,
                                                              @Param("referenceType") String referenceType,
                                                              @Param("referenceId") Long referenceId,
                                                              @Param("fromDateTime") LocalDateTime fromDateTime,
                                                              @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive,
                                                              Pageable pageable);

    @Query(value = """
            SELECT
              t.id AS transactionId,
              t.transaction_time AS transactionTime,
              m.id AS medicineId,
              m.brand AS medicineName,
              b.id AS batchId,
              b.batch_no AS batchNo,
              v.id AS vendorId,
              v.vendor_name AS vendorName,
              t.transaction_type AS transactionType,
              t.qty_in AS qtyIn,
              t.qty_out AS qtyOut,
              t.balance_after AS balanceAfter,
              t.unit_cost AS unitCost,
              t.unit_price AS unitPrice,
              t.reference_type AS referenceType,
              t.reference_id AS referenceId,
              t.reference_no AS referenceNo,
              t.created_by AS createdBy,
              t.note AS note
            FROM pharmacy_stock_transaction t
            INNER JOIN mdm_medicines m ON m.id = t.medicine_id
            LEFT JOIN pharmacy_stock_batch b ON b.id = t.batch_id
            LEFT JOIN pharmacy_vendor v ON v.id = b.vendor_id
            WHERE t.id = :transactionId
            """,
            nativeQuery = true)
    Optional<PharmacyTransactionDetailProjection> findTransactionDetailById(@Param("transactionId") Long transactionId);
}
