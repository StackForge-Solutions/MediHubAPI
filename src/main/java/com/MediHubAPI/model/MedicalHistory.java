package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "medical_histories")
public class MedicalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** JSON fields */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String personalHistory;   // JSON String

    @Lob
    @Column(columnDefinition = "TEXT")
    private String renalHistory;      // JSON String

    @Lob
    @Column(columnDefinition = "TEXT")
    private String diabetesHistory;   // JSON String

    @Lob
    @Column(columnDefinition = "TEXT")
    private String pastHistory;       // JSON String

    @Lob
    @Column(columnDefinition = "TEXT")
    private String otherHistories;    // JSON String

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_summary_id")
    private VisitSummary visitSummary;
}
