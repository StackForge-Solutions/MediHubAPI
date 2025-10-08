package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(of = {"subcategory", "name"})
@Entity
@Table(
        name = "symptoms",
        uniqueConstraints = @UniqueConstraint(name = "uk_symptom_subcat_name", columnNames = {"subcategory_id", "name"}),
        indexes = {
                @Index(name = "idx_symptom_name", columnList = "name"),
                @Index(name = "idx_symptom_subcategory_id", columnList = "subcategory_id")
        }
)
public class Symptom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_symptom_subcategory"))
    private SymptomSubcategory subcategory;

    @Column(nullable = false, length = 160)
    private String name; // e.g. "Fever"
}
