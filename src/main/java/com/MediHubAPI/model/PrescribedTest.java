package com.MediHubAPI.model;

import com.MediHubAPI.model.mdm.PathologyTestMaster;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prescribed_tests")
public class PrescribedTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;
    private Integer tat; // Turn-around time in hours
    private Integer quantity;
    private String notes;

    /** Link to Visit Summary */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_summary_id")
    private VisitSummary visitSummary;

    /** Link to Master Test (Optional) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mdm_pathology_tests_id")
    private PathologyTestMaster pathologyTestMaster;
}
