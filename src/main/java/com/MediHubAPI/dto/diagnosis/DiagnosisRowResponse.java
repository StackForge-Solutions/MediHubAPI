package com.MediHubAPI.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiagnosisRowResponse {
    private String name;
    private String sinceLabel;
    private Instant sinceDate;
    private Boolean chronic;
    private Boolean primary;
    private String comments;
}
