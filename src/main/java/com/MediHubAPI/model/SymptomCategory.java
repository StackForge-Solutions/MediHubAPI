 package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.Getter; import lombok.Setter;

 @Getter @Setter
@Entity @Table(
        name = "symptom_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_symptom_category_name", columnNames = "name"),
        indexes = @Index(name = "idx_symptom_category_name", columnList = "name")
)
public class SymptomCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name; // e.g. "General"
}

