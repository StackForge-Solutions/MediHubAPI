package com.MediHubAPI.model.billing;

import com.MediHubAPI.model.User;
import com.MediHubAPI.model.enums.ServiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(
        name = "doctor_service_items",
        uniqueConstraints = {
                // Ensure a doctor cannot have two services with the same name
                @UniqueConstraint(name = "uk_doctor_service_name", columnNames = {"doctor_id", "name"})
        },
        indexes = {
                @Index(name = "idx_dsi_doctor", columnList = "doctor_id"),
                @Index(name = "idx_dsi_name", columnList = "name"),
                @Index(name = "idx_dsi_status", columnList = "status")
        }
)
public class DoctorServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Points to a User with role=DOCTOR (not enforced here, but by business rules)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(nullable = false, length = 120)
    private String name;

    // Money-safe: BigDecimal, scale=2
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ServiceStatus status = ServiceStatus.ACTIVE;

    // Optional metadata
    @Column(length = 64)
    private String code;

    @Column(length = 1024)
    private String description;

    @Convert(converter = com.MediHubAPI.config.OffsetDateTimeUtcConverter.class)
    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Convert(converter = com.MediHubAPI.config.OffsetDateTimeUtcConverter.class)
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Column(nullable = false)
    private Boolean isDeleted = false;


}
