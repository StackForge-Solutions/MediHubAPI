package com.MediHubAPI.model.emr;

import com.MediHubAPI.model.Appointment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "emr_ip_admissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ip_admission_appointment", columnNames = {"appointment_id"})
        },
        indexes = {
                @Index(name = "idx_ip_admission_visit_date", columnList = "visit_date")
        }
)
public class IpAdmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "admission_advised", length = 8, nullable = false)
    private String admissionAdvised;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "admission_reason", length = 128)
    private String admissionReason;

    @Column(name = "tentative_stay_days")
    private Integer tentativeStayDays;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @PrePersist
    void onCreate() {
        if (savedAt == null) {
            savedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        savedAt = LocalDateTime.now();
    }
}
