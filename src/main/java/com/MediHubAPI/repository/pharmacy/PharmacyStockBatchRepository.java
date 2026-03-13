package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.PharmacyStockBatch;
import com.MediHubAPI.repository.projection.MedicineBatchProjection;
import com.MediHubAPI.repository.projection.PurchaseOrderReceiptHistoryProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PharmacyStockBatchRepository extends JpaRepository<PharmacyStockBatch, Long> {

    boolean existsByMedicine_IdAndBatchNoIgnoreCaseAndExpiryDate(Long medicineId, String batchNo, LocalDate expiryDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select batch
            from PharmacyStockBatch batch
            join fetch batch.medicine
            left join fetch batch.vendor
            where batch.id in :batchIds
            order by batch.id asc
            """)
    List<PharmacyStockBatch> findAllByIdForUpdate(@Param("batchIds") Collection<Long> batchIds);

    @Query(value = """
            SELECT
              b.id AS batchId,
              b.batch_no AS batchNo,
              v.id AS vendorId,
              v.vendor_name AS vendorName,
              b.expiry_date AS expiryDate,
              b.purchase_price AS purchasePrice,
              b.mrp AS mrp,
              b.selling_price AS sellingPrice,
              b.received_qty AS receivedQty,
              b.available_qty AS availableQty,
              CASE WHEN b.expiry_date < CURDATE() THEN TRUE ELSE FALSE END AS expired
            FROM pharmacy_stock_batch b
            LEFT JOIN pharmacy_vendor v ON v.id = b.vendor_id
            WHERE b.medicine_id = :medicineId
              AND (b.is_active = 1 OR b.is_active IS NULL)
              AND b.available_qty > 0
              AND (:includeExpired = TRUE OR b.expiry_date >= CURDATE())
            ORDER BY b.expiry_date ASC, b.id ASC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM pharmacy_stock_batch b
                    WHERE b.medicine_id = :medicineId
                      AND (b.is_active = 1 OR b.is_active IS NULL)
                      AND b.available_qty > 0
                      AND (:includeExpired = TRUE OR b.expiry_date >= CURDATE())
                    """,
            nativeQuery = true)
    Page<MedicineBatchProjection> findBatchPageByMedicineId(@Param("medicineId") Long medicineId,
                                                            @Param("includeExpired") boolean includeExpired,
                                                            Pageable pageable);

    @Query(value = """
            SELECT MIN(b.expiry_date)
            FROM pharmacy_stock_batch b
            WHERE b.medicine_id = :medicineId
              AND (b.is_active = 1 OR b.is_active IS NULL)
              AND b.available_qty > 0
              AND b.expiry_date >= CURDATE()
            """, nativeQuery = true)
    LocalDate findNearestExpiryDate(@Param("medicineId") Long medicineId);

    @Query(value = """
            SELECT COALESCE(SUM(b.available_qty * b.purchase_price), 0)
            FROM pharmacy_stock_batch b
            WHERE b.medicine_id = :medicineId
              AND (b.is_active = 1 OR b.is_active IS NULL)
              AND b.available_qty > 0
              AND b.expiry_date >= CURDATE()
            """, nativeQuery = true)
    BigDecimal computeStockValue(@Param("medicineId") Long medicineId);

    @Query(value = """
            SELECT
              b.id AS batchId,
              b.purchase_order_item_id AS purchaseOrderItemId,
              m.id AS medicineId,
              m.brand AS medicineName,
              b.batch_no AS batchNo,
              b.expiry_date AS expiryDate,
              b.received_qty AS receivedQty,
              b.purchase_price AS purchasePrice,
              b.mrp AS mrp,
              b.selling_price AS sellingPrice,
              b.received_at AS receivedAt
            FROM pharmacy_stock_batch b
            JOIN pharmacy_purchase_order_item poi ON poi.id = b.purchase_order_item_id
            JOIN mdm_medicines m ON m.id = b.medicine_id
            WHERE poi.purchase_order_id = :purchaseOrderId
            ORDER BY b.received_at DESC, b.id DESC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM pharmacy_stock_batch b
                    JOIN pharmacy_purchase_order_item poi ON poi.id = b.purchase_order_item_id
                    WHERE poi.purchase_order_id = :purchaseOrderId
                    """,
            nativeQuery = true)
    Page<PurchaseOrderReceiptHistoryProjection> findReceiptHistoryByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId,
                                                                                    Pageable pageable);
}
