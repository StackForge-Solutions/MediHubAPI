package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.ServiceStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DoctorServiceCreateRequest {
    @NotBlank
    @Size(max = 120)
    private String name;

    @NotNull
    @DecimalMin(value = "0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal price;

    @Size(max = 64)
    private String code;

    @Size(max = 1024)
    private String description;

    // optional – default ACTIVE
    private ServiceStatus status;
}

