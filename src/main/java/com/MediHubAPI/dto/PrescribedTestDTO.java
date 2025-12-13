package com.MediHubAPI.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescribedTestDTO {
    private Long id;
    private Long masterTestId;    // Reference to MasterTest (optional)
    private String name;
    private Double price;
    private Integer tat;
    private Integer quantity;
    private String notes;
    private Boolean isCustom;
}
