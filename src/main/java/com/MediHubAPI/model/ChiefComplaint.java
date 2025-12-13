package com.MediHubAPI.model;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "chief_complaints")
public class ChiefComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String complaint;
    private int years;
    private int months;
    private int weeks;
    private int days;
    private int sinceYear;
    private String bodyPart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_summary_id")
    private VisitSummary visitSummary;
}
