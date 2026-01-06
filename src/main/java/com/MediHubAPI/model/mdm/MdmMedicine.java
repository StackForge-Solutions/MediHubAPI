package com.MediHubAPI.model.mdm;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "mdm_medicines",
        indexes = {
                @Index(name = "idx_mdm_medicine_brand", columnList = "brand"),
                @Index(name = "idx_mdm_medicine_composition", columnList = "composition"),
                @Index(name = "idx_mdm_medicine_form", columnList = "form")
        }
)
public class MdmMedicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TAB / CAP / SYR / INJ / OINT / DROP
    @Column(nullable = false, length = 20)
    private String form;

    @Column(nullable = false, length = 255)
    private String brand;

    @Column(nullable = false, length = 255)
    private String composition;

    @Column(nullable = false)
    private Boolean isActive = true;

    // Optional: for integrations
    @Column(length = 64)
    private String code;
}
