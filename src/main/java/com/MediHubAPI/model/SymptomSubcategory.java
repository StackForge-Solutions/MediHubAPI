package com.MediHubAPI.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "symptom_subcategories",
        uniqueConstraints = @UniqueConstraint(name = "uk_subcategory_cat_name", columnNames = {"category_id", "name"}),
        indexes = {
                @Index(name = "idx_subcategory_name", columnList = "name"),
                @Index(name = "idx_subcategory_category_id", columnList = "category_id")
        }
)
public class SymptomSubcategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_subcategory_category"))
    private SymptomCategory category;

    @Column(nullable = false, length = 80)
    private String name; // e.g. "Systemic"
}
