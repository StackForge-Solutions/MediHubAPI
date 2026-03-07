package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "allergies")
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allergy_template_id")
    private Integer allergyTemplateId;

    @Column(name = "allergy_template_name", length = 255)
    private String allergyTemplateName;

    @Column(name = "allergy_category", length = 50)
    private String category;

    @Column(name = "allergy_language", length = 50)
    private String language;

    @Lob
    @Column(name = "allergies_text", columnDefinition = "TEXT")
    private String allergiesText;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_summary_id", nullable = false, unique = true)
    private VisitSummary visitSummary;
}
