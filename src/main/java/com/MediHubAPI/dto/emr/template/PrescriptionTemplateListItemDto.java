package com.MediHubAPI.dto.emr.template;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionTemplateListItemDto {
    private String id;        // tpl_123
    private String name;
    private String language;
    private Instant createdAt;
    private Instant updatedAt;
}
