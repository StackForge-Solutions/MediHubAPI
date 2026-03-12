package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.PharmacyStock;
import com.MediHubAPI.repository.projection.ManageStockRowProjection;
import com.MediHubAPI.repository.projection.StockSummaryProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PharmacyStockRepository extends JpaRepository<PharmacyStock, Long> {
    Optional<PharmacyStock> findByMedicine_Id(Long medicineId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select stock
            from PharmacyStock stock
            join fetch stock.medicine
            where stock.medicine.id in :medicineIds
            order by stock.medicine.id asc
            """)
    List<PharmacyStock> findAllByMedicineIdsForUpdate(@Param("medicineIds") Collection<Long> medicineIds);

    @Query(value = """
            SELECT
              m.id AS medicineId,
              COALESCE(m.code, CONCAT('MED-', m.id)) AS medicineCode,
              m.brand AS medicineName,
              m.brand AS brand,
              m.form AS form,
              COALESCE(s.available_qty, 0) AS availableQty,
              COALESCE(s.reserved_qty, 0) AS reservedQty,
              COALESCE(s.reorder_level, 0) AS reorderLevel,
              (
                SELECT b.selling_price
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
                ORDER BY b.expiry_date ASC, b.id ASC
                LIMIT 1
              ) AS sellingPrice,
              (
                SELECT b.mrp
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
                ORDER BY b.expiry_date ASC, b.id ASC
                LIMIT 1
              ) AS mrp,
              (
                SELECT MIN(b.expiry_date)
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
              ) AS nearestExpiryDate,
              COALESCE((
                SELECT SUM(b.available_qty * b.purchase_price)
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
              ), 0) AS stockValue
            FROM mdm_medicines m
            LEFT JOIN pharmacy_stock s ON s.medicine_id = m.id
            WHERE (m.is_active = 1 OR m.is_active IS NULL)
              AND (:form IS NULL OR m.form = :form)
              AND (
                    :q IS NULL
                    OR LOWER(m.brand) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(m.composition) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(m.code, '')) LIKE CONCAT('%', LOWER(:q), '%')
              )
              AND (
                    :vendorId IS NULL OR EXISTS (
                        SELECT 1
                        FROM pharmacy_stock_batch b
                        WHERE b.medicine_id = m.id
                          AND (b.is_active = 1 OR b.is_active IS NULL)
                          AND b.available_qty > 0
                          AND b.expiry_date >= CURDATE()
                          AND b.vendor_id = :vendorId
                    )
              )
              AND (:inStockOnly = FALSE OR COALESCE(s.available_qty, 0) > 0)
              AND (
                    :lowStockOnly = FALSE
                    OR (
                        COALESCE(s.available_qty, 0) > 0
                        AND COALESCE(s.available_qty, 0) <= COALESCE(s.reorder_level, 0)
                    )
              )
              AND (
                    :expiringInDays IS NULL OR EXISTS (
                        SELECT 1
                        FROM pharmacy_stock_batch b
                        WHERE b.medicine_id = m.id
                          AND (b.is_active = 1 OR b.is_active IS NULL)
                          AND b.available_qty > 0
                          AND b.expiry_date >= CURDATE()
                          AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL :expiringInDays DAY)
                          AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
                    )
              )
            ORDER BY
              CASE WHEN :sortField = 'medicineName' AND :sortDir = 'asc' THEN m.brand END ASC,
              CASE WHEN :sortField = 'medicineName' AND :sortDir = 'desc' THEN m.brand END DESC,
              CASE WHEN :sortField = 'brand' AND :sortDir = 'asc' THEN m.brand END ASC,
              CASE WHEN :sortField = 'brand' AND :sortDir = 'desc' THEN m.brand END DESC,
              CASE WHEN :sortField = 'form' AND :sortDir = 'asc' THEN m.form END ASC,
              CASE WHEN :sortField = 'form' AND :sortDir = 'desc' THEN m.form END DESC,
              CASE WHEN :sortField = 'availableQty' AND :sortDir = 'asc' THEN COALESCE(s.available_qty, 0) END ASC,
              CASE WHEN :sortField = 'availableQty' AND :sortDir = 'desc' THEN COALESCE(s.available_qty, 0) END DESC,
              CASE WHEN :sortField = 'nearestExpiryDate' AND :sortDir = 'asc' THEN (
                SELECT MIN(b.expiry_date)
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
              ) END ASC,
              CASE WHEN :sortField = 'nearestExpiryDate' AND :sortDir = 'desc' THEN (
                SELECT MIN(b.expiry_date)
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
              ) END DESC,
              CASE WHEN :sortField = 'stockValue' AND :sortDir = 'asc' THEN COALESCE((
                SELECT SUM(b.available_qty * b.purchase_price)
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
              ), 0) END ASC,
              CASE WHEN :sortField = 'stockValue' AND :sortDir = 'desc' THEN COALESCE((
                SELECT SUM(b.available_qty * b.purchase_price)
                FROM pharmacy_stock_batch b
                WHERE b.medicine_id = m.id
                  AND (b.is_active = 1 OR b.is_active IS NULL)
                  AND b.available_qty > 0
                  AND b.expiry_date >= CURDATE()
                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
              ), 0) END DESC,
              m.brand ASC,
              m.id ASC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM mdm_medicines m
                    LEFT JOIN pharmacy_stock s ON s.medicine_id = m.id
                    WHERE (m.is_active = 1 OR m.is_active IS NULL)
                      AND (:form IS NULL OR m.form = :form)
                      AND (
                            :q IS NULL
                            OR LOWER(m.brand) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(m.composition) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(m.code, '')) LIKE CONCAT('%', LOWER(:q), '%')
                      )
                      AND (
                            :vendorId IS NULL OR EXISTS (
                                SELECT 1
                                FROM pharmacy_stock_batch b
                                WHERE b.medicine_id = m.id
                                  AND (b.is_active = 1 OR b.is_active IS NULL)
                                  AND b.available_qty > 0
                                  AND b.expiry_date >= CURDATE()
                                  AND b.vendor_id = :vendorId
                            )
                      )
                      AND (:inStockOnly = FALSE OR COALESCE(s.available_qty, 0) > 0)
                      AND (
                            :lowStockOnly = FALSE
                            OR (
                                COALESCE(s.available_qty, 0) > 0
                                AND COALESCE(s.available_qty, 0) <= COALESCE(s.reorder_level, 0)
                            )
                      )
                      AND (
                            :expiringInDays IS NULL OR EXISTS (
                                SELECT 1
                                FROM pharmacy_stock_batch b
                                WHERE b.medicine_id = m.id
                                  AND (b.is_active = 1 OR b.is_active IS NULL)
                                  AND b.available_qty > 0
                                  AND b.expiry_date >= CURDATE()
                                  AND b.expiry_date <= DATE_ADD(CURDATE(), INTERVAL :expiringInDays DAY)
                                  AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
                            )
                      )
                    """,
            nativeQuery = true)
    Page<ManageStockRowProjection> searchManageStocks(@Param("q") String q,
                                                      @Param("form") String form,
                                                      @Param("inStockOnly") boolean inStockOnly,
                                                      @Param("lowStockOnly") boolean lowStockOnly,
                                                      @Param("expiringInDays") Integer expiringInDays,
                                                      @Param("vendorId") Long vendorId,
                                                      @Param("sortField") String sortField,
                                                      @Param("sortDir") String sortDir,
                                                      Pageable pageable);

    @Query(value = """
            SELECT
              COUNT(*) AS totalMedicines,
              SUM(CASE WHEN x.availableQty > 0 AND x.availableQty <= x.reorderLevel THEN 1 ELSE 0 END) AS lowStockCount,
              SUM(CASE WHEN x.availableQty <= 0 THEN 1 ELSE 0 END) AS outOfStockCount,
              SUM(CASE WHEN x.nearestExpiryDate IS NOT NULL AND x.nearestExpiryDate <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS expiringSoonCount,
              COALESCE(SUM(x.stockValue), 0) AS stockValue
            FROM (
              SELECT
                m.id AS medicineId,
                COALESCE(s.available_qty, 0) AS availableQty,
                COALESCE(s.reorder_level, 0) AS reorderLevel,
                (
                  SELECT MIN(b.expiry_date)
                  FROM pharmacy_stock_batch b
                  WHERE b.medicine_id = m.id
                    AND (b.is_active = 1 OR b.is_active IS NULL)
                    AND b.available_qty > 0
                    AND b.expiry_date >= CURDATE()
                    AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
                ) AS nearestExpiryDate,
                COALESCE((
                  SELECT SUM(b.available_qty * b.purchase_price)
                  FROM pharmacy_stock_batch b
                  WHERE b.medicine_id = m.id
                    AND (b.is_active = 1 OR b.is_active IS NULL)
                    AND b.available_qty > 0
                    AND b.expiry_date >= CURDATE()
                    AND (:vendorId IS NULL OR b.vendor_id = :vendorId)
                ), 0) AS stockValue
              FROM mdm_medicines m
              LEFT JOIN pharmacy_stock s ON s.medicine_id = m.id
              WHERE (m.is_active = 1 OR m.is_active IS NULL)
                AND (:form IS NULL OR m.form = :form)
                AND (
                      :q IS NULL
                      OR LOWER(m.brand) LIKE CONCAT('%', LOWER(:q), '%')
                      OR LOWER(m.composition) LIKE CONCAT('%', LOWER(:q), '%')
                      OR LOWER(COALESCE(m.code, '')) LIKE CONCAT('%', LOWER(:q), '%')
                )
                AND (
                      :vendorId IS NULL OR EXISTS (
                          SELECT 1
                          FROM pharmacy_stock_batch b
                          WHERE b.medicine_id = m.id
                            AND (b.is_active = 1 OR b.is_active IS NULL)
                            AND b.available_qty > 0
                            AND b.expiry_date >= CURDATE()
                            AND b.vendor_id = :vendorId
                      )
                )
            ) x
            """, nativeQuery = true)
    StockSummaryProjection summarizeManageStocks(@Param("q") String q,
                                                 @Param("vendorId") Long vendorId,
                                                 @Param("form") String form);
}
