package com.MediHubAPI.model.pharmacy;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "pharmacy_vendor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_pharmacy_vendor_code", columnNames = "vendor_code")
        },
        indexes = {
                @Index(name = "idx_pharmacy_vendor_name", columnList = "vendor_name"),
                @Index(name = "idx_pharmacy_vendor_code", columnList = "vendor_code"),
                @Index(name = "idx_pharmacy_vendor_phone", columnList = "phone"),
                @Index(name = "idx_pharmacy_vendor_gst_no", columnList = "gst_no"),
                @Index(name = "idx_pharmacy_vendor_city", columnList = "city")
        }
)
public class PharmacyVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_name", nullable = false, length = 255)
    private String vendorName;

    @Column(name = "vendor_code", nullable = false, length = 50)
    private String vendorCode;

    @Column(name = "contact_person", length = 255)
    private String contactPerson;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "gst_no", length = 30)
    private String gstNo;

    @Column(name = "drug_license_no", length = 50)
    private String drugLicenseNo;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 20)
    private String pincode;

    @Column(name = "payment_terms_days", nullable = false)
    private Integer paymentTermsDays = 0;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
