package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.mdm.MdmMedicine;
import com.MediHubAPI.repository.projection.MedicineSearchRowProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MdmMedicineRepository extends JpaRepository<MdmMedicine, Long> {

    @Query(value = """
        SELECT
          m.id AS id,
          m.form AS form,
          m.brand AS brand,
          m.composition AS composition,
          COALESCE(s.available_qty, 0) AS stockQty,
          CASE WHEN COALESCE(s.available_qty, 0) > 0 THEN 1 ELSE 0 END AS inStock
        FROM mdm_medicines m
        LEFT JOIN pharmacy_stock s ON s.medicine_id = m.id
        WHERE (m.is_active = 1 OR m.is_active IS NULL)
          AND (:form IS NULL OR m.form = :form)
          AND LOWER(m.brand) LIKE CONCAT(LOWER(:q), '%')
          AND (:inStockOnly = FALSE OR COALESCE(s.available_qty, 0) > 0)
        ORDER BY COALESCE(s.available_qty, 0) DESC, m.brand ASC
        """, nativeQuery = true)
    List<MedicineSearchRowProjection> searchByBrand(
            @Param("q") String q,
            @Param("form") String form,
            @Param("inStockOnly") boolean inStockOnly,
            Pageable pageable
    );

    @Query(value = """
        SELECT
          m.id AS id,
          m.form AS form,
          m.brand AS brand,
          m.composition AS composition,
          COALESCE(s.available_qty, 0) AS stockQty,
          CASE WHEN COALESCE(s.available_qty, 0) > 0 THEN TRUE ELSE FALSE END AS inStock
        FROM mdm_medicines m
        LEFT JOIN pharmacy_stock s ON s.medicine_id = m.id
        WHERE (m.is_active = 1 OR m.is_active IS NULL)
          AND (:form IS NULL OR m.form = :form)
          AND LOWER(m.composition) LIKE CONCAT('%', LOWER(:q), '%')
          AND (:inStockOnly = FALSE OR COALESCE(s.available_qty, 0) > 0)
        ORDER BY COALESCE(s.available_qty, 0) DESC, m.brand ASC
        """, nativeQuery = true)
    List<MedicineSearchRowProjection> searchByComposition(
            @Param("q") String q,
            @Param("form") String form,
            @Param("inStockOnly") boolean inStockOnly,
            Pageable pageable
    );
}
