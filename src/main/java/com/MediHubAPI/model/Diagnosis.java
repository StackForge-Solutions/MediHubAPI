package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "diagnoses",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"visit_summary_id", "source", "name"})
        }
)
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private Integer years;

    @Column(nullable = false)
    private Integer months;

    @Column(nullable = false)
    private Integer days;

    @Column(name = "since_year", nullable = false)
    private Integer sinceYear;

    @Column(name = "is_chronic", nullable = false)
    private Boolean chronic;

    @Column(name = "is_primary", nullable = false)
    private Boolean primaryDiagnosis;

    @Column(length = 1000)
    private String comments;

    @Column(
            name = "primary_guard",
            columnDefinition = "bigint GENERATED ALWAYS AS (case when is_primary then visit_summary_id else null end) STORED",
            insertable = false,
            updatable = false,
            unique = true
    )
    private Long primaryGuard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_summary_id", nullable = false)
    private VisitSummary visitSummary;
}
