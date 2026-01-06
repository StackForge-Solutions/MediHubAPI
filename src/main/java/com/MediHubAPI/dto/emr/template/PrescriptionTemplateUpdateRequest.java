package com.MediHubAPI.dto.emr.template;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionTemplateUpdateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String language;

    @NotNull
    private JsonNode payload;
}
