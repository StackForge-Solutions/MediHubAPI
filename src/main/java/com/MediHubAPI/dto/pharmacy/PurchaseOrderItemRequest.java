package com.MediHubAPI.dto.pharmacy;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItemRequest {

    @NotNull(message = "medicineId is required")
    @Positive(message = "medicineId must be greater than 0")
    private Long medicineId;

    @NotNull(message = "orderedQty is required")
    @Min(value = 1, message = "orderedQty must be greater than 0")
    private Integer orderedQty;

    @NotNull(message = "purchasePrice is required")
    @DecimalMin(value = "0.00", message = "purchasePrice must be greater than or equal to 0")
    private BigDecimal purchasePrice;

    @NotNull(message = "mrp is required")
    @DecimalMin(value = "0.00", message = "mrp must be greater than or equal to 0")
    private BigDecimal mrp;

    @NotNull(message = "sellingPrice is required")
    @DecimalMin(value = "0.00", message = "sellingPrice must be greater than or equal to 0")
    private BigDecimal sellingPrice;

    @DecimalMin(value = "0.00", message = "taxPercent must be greater than or equal to 0")
    private BigDecimal taxPercent;

    @DecimalMin(value = "0.00", message = "discountPercent must be greater than or equal to 0")
    private BigDecimal discountPercent;
}
