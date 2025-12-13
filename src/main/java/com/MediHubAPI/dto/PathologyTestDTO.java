package com.MediHubAPI.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathologyTestDTO {
    private Long id;
    private String name;
    private Double price;
    private Integer tat;
    private String category;
    private String notes;
    private Boolean isActive;
}
