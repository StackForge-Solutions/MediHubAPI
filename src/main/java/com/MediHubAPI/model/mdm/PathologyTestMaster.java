package com.MediHubAPI.model.mdm;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mdm_pathology_tests")
public class PathologyTestMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g., "TSH", "Lipid Profile", "HbA1c" */
    @Column(nullable = false, unique = true)
    private String name;

    /** Department or group like 'Biochemistry', 'Hormone', 'Infectious' */
    private String category;

    /** Price in ₹ (or your system currency) */
    private Double price;

    /** Turn-around time in hours */
    private Integer tat;

    /** Optional subcategory or panel info (e.g., “Liver Function Panel”) */
    private String subCategory;

    /** Reference notes for doctor or lab */
    @Column(length = 1000)
    private String notes;

    /** Whether this test is currently active in the catalog */
    private Boolean isActive = true;

    /** Optional code (useful for future integration with LOINC or HIS) */
    private String code;
}
