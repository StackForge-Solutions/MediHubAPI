package com.MediHubAPI.model;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "visit_summaries")
public class VisitSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Doctor reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private User doctor;

    // Patient reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id")
    private User patient;

    private String visitTime;
    private String visitDate;

    @OneToMany(mappedBy = "visitSummary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiefComplaint> chiefComplaints;
}

