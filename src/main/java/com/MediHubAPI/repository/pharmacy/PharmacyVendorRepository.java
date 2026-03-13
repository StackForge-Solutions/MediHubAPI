package com.MediHubAPI.repository.pharmacy;

import com.MediHubAPI.model.pharmacy.PharmacyVendor;
import com.MediHubAPI.repository.projection.PharmacyVendorRowProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PharmacyVendorRepository extends JpaRepository<PharmacyVendor, Long> {

    boolean existsByVendorCodeIgnoreCase(String vendorCode);

    boolean existsByVendorCodeIgnoreCaseAndIdNot(String vendorCode, Long id);

    @Query(value = """
            SELECT
              v.id AS vendorId,
              v.vendor_code AS vendorCode,
              v.vendor_name AS vendorName,
              v.contact_person AS contactPerson,
              v.phone AS phone,
              v.email AS email,
              v.gst_no AS gstNo,
              v.city AS city,
              v.payment_terms_days AS paymentTermsDays,
              v.active AS active
            FROM pharmacy_vendor v
            WHERE (
                    :q IS NULL
                    OR LOWER(v.vendor_name) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(v.vendor_code) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(v.phone, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    OR LOWER(COALESCE(v.gst_no, '')) LIKE CONCAT('%', LOWER(:q), '%')
            )
              AND (:active IS NULL OR v.active = :active)
            ORDER BY
              CASE WHEN :sortField = 'vendorName' AND :sortDir = 'asc' THEN v.vendor_name END ASC,
              CASE WHEN :sortField = 'vendorName' AND :sortDir = 'desc' THEN v.vendor_name END DESC,
              CASE WHEN :sortField = 'vendorCode' AND :sortDir = 'asc' THEN v.vendor_code END ASC,
              CASE WHEN :sortField = 'vendorCode' AND :sortDir = 'desc' THEN v.vendor_code END DESC,
              CASE WHEN :sortField = 'city' AND :sortDir = 'asc' THEN v.city END ASC,
              CASE WHEN :sortField = 'city' AND :sortDir = 'desc' THEN v.city END DESC,
              CASE WHEN :sortField = 'paymentTermsDays' AND :sortDir = 'asc' THEN v.payment_terms_days END ASC,
              CASE WHEN :sortField = 'paymentTermsDays' AND :sortDir = 'desc' THEN v.payment_terms_days END DESC,
              CASE WHEN :sortField = 'active' AND :sortDir = 'asc' THEN v.active END ASC,
              CASE WHEN :sortField = 'active' AND :sortDir = 'desc' THEN v.active END DESC,
              v.vendor_name ASC,
              v.id ASC
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM pharmacy_vendor v
                    WHERE (
                            :q IS NULL
                            OR LOWER(v.vendor_name) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(v.vendor_code) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(v.phone, '')) LIKE CONCAT('%', LOWER(:q), '%')
                            OR LOWER(COALESCE(v.gst_no, '')) LIKE CONCAT('%', LOWER(:q), '%')
                    )
                      AND (:active IS NULL OR v.active = :active)
                    """,
            nativeQuery = true)
    Page<PharmacyVendorRowProjection> searchVendors(@Param("q") String q,
                                                    @Param("active") Boolean active,
                                                    @Param("sortField") String sortField,
                                                    @Param("sortDir") String sortDir,
                                                    Pageable pageable);
}
