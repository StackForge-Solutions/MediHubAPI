package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "vitals")
public class Vitals {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double height;
    private Double weight;
    private Double bmi;
    private Double waist;
    private Integer bpSystolic;
    private Integer bpDiastolic;
    private Integer pulse;
    private Double hc;             // Head Circumference
    private Double temperature;
    private Integer respiratory;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_summary_id")
    private VisitSummary visitSummary;
}
