package com.MediHubAPI.dto.emr.template;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionTemplateDetailsDto {
    private String id;
    private String name;
    private String language;
    private Instant createdAt;
    private Instant updatedAt;
    private JsonNode payload;
}
