package com.MediHubAPI.dto.emr.template;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionTemplateListDataDto {
    private List<PrescriptionTemplateListItemDto> items;
    private int count;
    private int limit;
    private String query;
}
