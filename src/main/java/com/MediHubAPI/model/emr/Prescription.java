package com.MediHubAPI.model.emr;

import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.VisitSummary;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "prescriptions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_prescription_appointment", columnNames = {"appointment_id"})
        },
        indexes = {
                @Index(name = "idx_prescription_visit_summary", columnList = "visit_summary_id")
        }
)
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* Soft link: keep appointment_id for fast lookup and unique constraint */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_summary_id", nullable = false)
    private VisitSummary visitSummary;

    @Column(length = 32)
    private String language; // "English"

    @Column(name = "follow_up_enabled", nullable = false)
    private Boolean followUpEnabled = false;

    @Column(name = "follow_up_duration")
    private Integer followUpDuration;

    @Column(name = "follow_up_unit", length = 16)
    private String followUpUnit; // "Days", "Weeks", "Months"

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "send_follow_up_email", nullable = false)
    private Boolean sendFollowUpEmail = false;

    @Column(name = "advice_text", columnDefinition = "TEXT")
    private String adviceText;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionMedication> medications = new ArrayList<>();

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    @PrePersist
    void onCreate() {
        if (savedAt == null) savedAt = LocalDateTime.now();
        if (followUpEnabled == null) followUpEnabled = false;
        if (sendFollowUpEmail == null) sendFollowUpEmail = false;
    }

    @PreUpdate
    void onUpdate() {
        savedAt = LocalDateTime.now();
    }
}
