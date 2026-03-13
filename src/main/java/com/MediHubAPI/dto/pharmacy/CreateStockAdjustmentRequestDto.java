package com.MediHubAPI.dto.pharmacy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStockAdjustmentRequestDto {

    @NotNull(message = "adjustmentDate is required")
    private LocalDate adjustmentDate;

    @NotBlank(message = "adjustmentType is required")
    @Size(max = 30, message = "adjustmentType must not exceed 30 characters")
    private String adjustmentType;

    @NotBlank(message = "reason is required")
    @Size(max = 50, message = "reason must not exceed 50 characters")
    private String reason;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;

    @Valid
    @NotEmpty(message = "items must not be empty")
    private List<CreateStockAdjustmentItemRequestDto> items;
}
