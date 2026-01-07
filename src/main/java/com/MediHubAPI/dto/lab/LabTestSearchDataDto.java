package com.MediHubAPI.dto.lab;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabTestSearchDataDto {
    private List<LabTestSearchItemDto> items;
    private Integer count;
    private Integer limit;
    private String query;
}
