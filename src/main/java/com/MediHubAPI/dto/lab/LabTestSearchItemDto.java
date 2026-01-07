package com.MediHubAPI.dto.lab;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTestSearchItemDto {
    private Long id;
    private String name;
    private Double price;
    private Integer tat; // hours
}
