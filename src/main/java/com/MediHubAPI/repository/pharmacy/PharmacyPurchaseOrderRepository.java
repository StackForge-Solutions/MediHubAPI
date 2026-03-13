package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.PharmacyPurchaseOrder;
import com.MediHubAPI.repository.projection.PurchaseOrderRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;

public interface PharmacyPurchaseOrderRepository extends JpaRepository<PharmacyPurchaseOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select po from PharmacyPurchaseOrder po join fetch po.vendor where po.id = :id")
    Optional<PharmacyPurchaseOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select po
            from PharmacyPurchaseOrder po
            join fetch po.vendor
            where po.id = :id
            """)
    Optional<PharmacyPurchaseOrder> findByIdWithVendor(@Param("id") Long id);

    @Query(value = """
            SELECT
              po.id AS purchaseOrderId,
              po.po_number AS poNumber,
              v.id AS vendorId,
              v.vendor_name AS vendorName,
              po.order_date AS orderDate,
              po.status AS status,
              COUNT(poi.id) AS itemCount,
              COALESCE(SUM(poi.ordered_qty), 0) AS orderedQty,
              COALESCE(SUM(poi.received_qty), 0) AS receivedQty,
              COALESCE(po.net_amount, 0) AS netAmount
            FROM pharmacy_purchase_order po
            JOIN pharmacy_vendor v ON v.id = po.vendor_id
            LEFT JOIN pharmacy_purchase_order_item poi ON poi.purchase_order_id = po.id
            WHERE (
                    :q IS NULL
                    OR LOWER(po.po_number) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(v.vendor_name) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(po.invoice_number, '')) LIKE CONCAT('%', LOWER(:q), '%')
            )
              AND (:status IS NULL OR po.status = :status)
              AND (:vendorId IS NULL OR po.vendor_id = :vendorId)
              AND (:fromDate IS NULL OR po.order_date >= :fromDate)
              AND (:toDate IS NULL OR po.order_date <= :toDate)
            GROUP BY po.id, po.po_number, v.id, v.vendor_name, po.order_date, po.status, po.net_amount
            ORDER BY
              CASE WHEN :sortField = 'poNumber' AND :sortDir = 'asc' THEN po.po_number END ASC,
              CASE WHEN :sortField = 'poNumber' AND :sortDir = 'desc' THEN po.po_number END DESC,
              CASE WHEN :sortField = 'vendorName' AND :sortDir = 'asc' THEN v.vendor_name END ASC,
              CASE WHEN :sortField = 'vendorName' AND :sortDir = 'desc' THEN v.vendor_name END DESC,
              CASE WHEN :sortField = 'orderDate' AND :sortDir = 'asc' THEN po.order_date END ASC,
              CASE WHEN :sortField = 'orderDate' AND :sortDir = 'desc' THEN po.order_date END DESC,
              CASE WHEN :sortField = 'status' AND :sortDir = 'asc' THEN po.status END ASC,
              CASE WHEN :sortField = 'status' AND :sortDir = 'desc' THEN po.status END DESC,
              CASE WHEN :sortField = 'netAmount' AND :sortDir = 'asc' THEN po.net_amount END ASC,
              CASE WHEN :sortField = 'netAmount' AND :sortDir = 'desc' THEN po.net_amount END DESC,
              po.order_date DESC,
              po.id DESC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM pharmacy_purchase_order po
                    JOIN pharmacy_vendor v ON v.id = po.vendor_id
                    WHERE (
                            :q IS NULL
                            OR LOWER(po.po_number) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(v.vendor_name) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(po.invoice_number, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    )
                      AND (:status IS NULL OR po.status = :status)
                      AND (:vendorId IS NULL OR po.vendor_id = :vendorId)
                      AND (:fromDate IS NULL OR po.order_date >= :fromDate)
                      AND (:toDate IS NULL OR po.order_date <= :toDate)
                    """,
            nativeQuery = true)
    Page<PurchaseOrderRowProjection> searchPurchaseOrders(@Param("q") String q,
                                                          @Param("status") String status,
                                                          @Param("vendorId") Long vendorId,
                                                          @Param("fromDate") LocalDate fromDate,
                                                          @Param("toDate") LocalDate toDate,
                                                          @Param("sortField") String sortField,
                                                          @Param("sortDir") String sortDir,
                                                          Pageable pageable);
}
