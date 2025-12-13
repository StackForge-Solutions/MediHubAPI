package com.MediHubAPI.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BulkPatchRequest {
    public enum Op { ACTIVATE, INACTIVATE, PERCENT, ABSOLUTE }

    @NotEmpty
    private List<Long> ids;

    @NotNull
    private Op op;

    // For PERCENT (positive or negative) – e.g., +5 or -3.5
    @Digits(integer = 3, fraction = 2)
    private BigDecimal percent; // nullable except when op=PERCENT

    // For ABSOLUTE – set price to this exact amount
    @Digits(integer = 10, fraction = 2)
    private BigDecimal absolute; // nullable except when op=ABSOLUTE
}
