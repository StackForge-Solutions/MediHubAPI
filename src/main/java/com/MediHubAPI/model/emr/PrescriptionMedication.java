package com.MediHubAPI.model.emr;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "prescription_medications",
        indexes = {
                @Index(name = "idx_pm_prescription", columnList = "prescription_id"),
                @Index(name = "idx_pm_medicine_id", columnList = "medicine_id")
        }
)
public class PrescriptionMedication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medicine_id")
    private Long medicineId;

    @Column(length = 16)
    private String form; // "TAB"

    @Column(length = 16)
    private String mode; // "BRAND"

    @Column(name = "medicine_name", length = 128)
    private String medicineName;

    @Column(length = 256)
    private String composition;

    @Column(name = "in_stock")
    private Boolean inStock;

    @Column(name = "stock_qty")
    private Integer stockQty;

    // dose schedule
    @Column(name = "m_dose")
    private Integer m; // morning

    @Column(name = "a_dose")
    private Integer a; // afternoon

    @Column(name = "n_dose")
    private Integer n; // night

    // SOS
    @Column(name = "sos_enabled")
    private Boolean sosEnabled;

    @Column(name = "sos_count")
    private Integer sosCount;

    @Column(name = "sos_unit", length = 16)
    private String sosUnit; // "times"

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "lifelong")
    private Boolean lifelong;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "duration_unit", length = 16)
    private String durationUnit; // "Days"

    @Column(name = "periodicity", length = 16)
    private String periodicity; // "DAILY"
}
