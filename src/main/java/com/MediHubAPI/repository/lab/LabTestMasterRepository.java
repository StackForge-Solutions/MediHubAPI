package com.MediHubAPI.repository.lab;

import com.MediHubAPI.model.mdm.PathologyTestMaster;
import com.MediHubAPI.repository.projection.LabTestMasterRowProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LabTestMasterRepository extends JpaRepository<PathologyTestMaster, Long> {

    @Query(value = """
        SELECT
          COALESCE(t.code, CONCAT('LAB_', t.id)) AS code,
          t.name                                 AS name,
          COALESCE(t.price, 0)                   AS amount,
          1                                      AS taxable,
          t.tat                                  AS tatHours,
          NULL                                   AS sampleType,
          COALESCE(t.is_active, 1)               AS active,
          NULL                                   AS updatedAtISO
        FROM mdm_pathology_tests t
        WHERE
          (:active IS NULL OR COALESCE(t.is_active, 1) = :active)
          AND (
            :q IS NULL OR :q = '' OR
            LOWER(t.name) LIKE CONCAT(LOWER(:q), '%') OR
            LOWER(COALESCE(t.code, '')) LIKE CONCAT(LOWER(:q), '%')
          )
        ORDER BY
          CASE WHEN :sort = 'updatedAt:desc' THEN 1 ELSE 0 END DESC,
          t.name ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<LabTestMasterRowProjection> fetchMaster(
            @Param("active") Integer active,     // 1/0/null
            @Param("q") String q,
            @Param("limit") int limit,
            @Param("offset") int offset,
            @Param("sort") String sort
    );
}
