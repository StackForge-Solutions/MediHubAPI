package com.MediHubAPI.dto.pharmacy;

import jakarta.validation.Valid;
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
public class PurchaseOrderReceiveRequest {

    @NotNull(message = "receiptDate is required")
    private LocalDate receiptDate;

    @Size(max = 100, message = "invoiceNumber must not exceed 100 characters")
    private String invoiceNumber;

    private LocalDate invoiceDate;

    @Size(max = 1000, message = "note must not exceed 1000 characters")
    private String note;

    @Valid
    @NotEmpty(message = "items must not be empty")
    private List<PurchaseOrderReceiveRequestItem> items;
}
