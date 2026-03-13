package com.MediHubAPI.dto.pharmacy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAdjustmentItemRequestDto {

    @NotNull(message = "medicineId is required")
    @Positive(message = "medicineId must be greater than 0")
    private Long medicineId;

    @NotNull(message = "batchId is required")
    @Positive(message = "batchId must be greater than 0")
    private Long batchId;

    @NotNull(message = "qty is required")
    @Positive(message = "qty must be greater than 0")
    private Integer qty;

    @DecimalMin(value = "0.0", inclusive = true, message = "unitCost must be greater than or equal to 0")
    private BigDecimal unitCost;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;
}
