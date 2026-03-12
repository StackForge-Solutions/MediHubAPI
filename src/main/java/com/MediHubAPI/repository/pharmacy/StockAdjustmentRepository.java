package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.StockAdjustment;
import com.MediHubAPI.repository.projection.StockAdjustmentListRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    @Query(value = """
            SELECT
              sa.id AS adjustmentId,
              sa.adjustment_no AS adjustmentNo,
              sa.adjustment_date AS adjustmentDate,
              sa.adjustment_type AS adjustmentType,
              sa.reason AS reason,
              COUNT(DISTINCT sai.medicine_id) AS medicineCount,
              COALESCE(SUM(sai.qty), 0) AS totalQtyImpact,
              sa.created_by AS createdBy,
              sa.status AS status
            FROM stock_adjustment sa
            LEFT JOIN stock_adjustment_item sai ON sai.stock_adjustment_id = sa.id
            WHERE (
                    :q IS NULL
                    OR LOWER(sa.adjustment_no) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(sa.note, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR EXISTS (
                        SELECT 1
                        FROM stock_adjustment_item sai2
                        JOIN mdm_medicines m ON m.id = sai2.medicine_id
                        WHERE sai2.stock_adjustment_id = sa.id
                          AND LOWER(m.brand) LIKE CONCAT('%', LOWER(:q), '%')
                    )
            )
              AND (:reason IS NULL OR sa.reason = :reason)
              AND (:type IS NULL OR sa.adjustment_type = :type)
              AND (:fromDate IS NULL OR sa.adjustment_date >= :fromDate)
              AND (:toDate IS NULL OR sa.adjustment_date <= :toDate)
            GROUP BY sa.id, sa.adjustment_no, sa.adjustment_date, sa.adjustment_type, sa.reason, sa.created_by, sa.status
            ORDER BY sa.adjustment_date DESC, sa.id DESC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM stock_adjustment sa
                    WHERE (
                            :q IS NULL
                            OR LOWER(sa.adjustment_no) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(sa.note, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR EXISTS (
                                SELECT 1
                                FROM stock_adjustment_item sai2
                                JOIN mdm_medicines m ON m.id = sai2.medicine_id
                                WHERE sai2.stock_adjustment_id = sa.id
                                  AND LOWER(m.brand) LIKE CONCAT('%', LOWER(:q), '%')
                            )
                    )
                      AND (:reason IS NULL OR sa.reason = :reason)
                      AND (:type IS NULL OR sa.adjustment_type = :type)
                      AND (:fromDate IS NULL OR sa.adjustment_date >= :fromDate)
                      AND (:toDate IS NULL OR sa.adjustment_date <= :toDate)
                    """,
            nativeQuery = true)
    Page<StockAdjustmentListRowProjection> searchAdjustments(@Param("q") String q,
                                                             @Param("reason") String reason,
                                                             @Param("type") String type,
                                                             @Param("fromDate") LocalDate fromDate,
                                                             @Param("toDate") LocalDate toDate,
                                                             Pageable pageable);
}
