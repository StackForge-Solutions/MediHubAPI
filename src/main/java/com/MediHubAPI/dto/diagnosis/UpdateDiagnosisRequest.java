package com.MediHubAPI.dto.diagnosis;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDiagnosisRequest {

    @Positive(message = "appointmentId must be a positive integer")
    private Long appointmentId;

    @NotBlank(message = "currentName is required")
    @Size(max = 255, message = "currentName must not exceed 255 characters")
    private String currentName;

    @NotBlank(message = "Source is required")
    @Size(max = 50, message = "source must not exceed 50 characters")
    private String source;

    @NotBlank(message = "Diagnosis name is required")
    @Size(max = 255, message = "name must not exceed 255 characters")
    private String name;

    @NotNull(message = "years is required")
    @Min(value = 0, message = "years must be greater than or equal to 0")
    private Integer years;

    @NotNull(message = "months is required")
    @Min(value = 0, message = "months must be greater than or equal to 0")
    private Integer months;

    @NotNull(message = "days is required")
    @Min(value = 0, message = "days must be greater than or equal to 0")
    private Integer days;

    @Min(value = 1900, message = "sinceYear must be greater than or equal to 1900")
    private Integer sinceYear;

    @NotNull(message = "chronic is required")
    private Boolean chronic;

    @NotNull(message = "primary is required")
    private Boolean primary;

    @Size(max = 1000, message = "comments must not exceed 1000 characters")
    private String comments;
}
