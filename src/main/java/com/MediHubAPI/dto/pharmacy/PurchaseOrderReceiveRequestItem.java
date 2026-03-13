package com.MediHubAPI.dto.pharmacy;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderReceiveRequestItem {

    @NotNull(message = "purchaseOrderItemId is required")
    @Positive(message = "purchaseOrderItemId must be greater than 0")
    private Long purchaseOrderItemId;

    @NotBlank(message = "batchNo is required")
    @Size(max = 100, message = "batchNo must not exceed 100 characters")
    private String batchNo;

    @NotNull(message = "expiryDate is required")
    private LocalDate expiryDate;

    @NotNull(message = "receivedQty is required")
    @Min(value = 1, message = "receivedQty must be greater than 0")
    private Integer receivedQty;

    @NotNull(message = "purchasePrice is required")
    @DecimalMin(value = "0.00", message = "purchasePrice must be greater than or equal to 0")
    private BigDecimal purchasePrice;

    @NotNull(message = "mrp is required")
    @DecimalMin(value = "0.00", message = "mrp must be greater than or equal to 0")
    private BigDecimal mrp;

    @NotNull(message = "sellingPrice is required")
    @DecimalMin(value = "0.00", message = "sellingPrice must be greater than or equal to 0")
    private BigDecimal sellingPrice;

    @Size(max = 500, message = "note must not exceed 500 characters")
    private String note;
}
