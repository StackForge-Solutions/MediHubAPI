package com.MediHubAPI.dto.pharmacy;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderCreateRequest {

    @NotNull(message = "vendorId is required")
    @Positive(message = "vendorId must be greater than 0")
    private Long vendorId;

    @NotNull(message = "orderDate is required")
    private LocalDate orderDate;

    @Size(max = 100, message = "invoiceNumber must not exceed 100 characters")
    private String invoiceNumber;

    private LocalDate invoiceDate;

    @Size(max = 1000, message = "note must not exceed 1000 characters")
    private String note;

    @Valid
    @NotEmpty(message = "items must not be empty")
    private List<PurchaseOrderItemRequest> items;
}
