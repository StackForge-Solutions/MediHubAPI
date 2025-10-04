package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.ServiceStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorServiceUpdateRequest {
    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    private ServiceStatus status; // ACTIVE / INACTIVE

    @Size(max = 64)
    private String code;

    @Size(max = 1024)
    private String description;

    @Builder.Default
    private Boolean isDeleted = false;
}
