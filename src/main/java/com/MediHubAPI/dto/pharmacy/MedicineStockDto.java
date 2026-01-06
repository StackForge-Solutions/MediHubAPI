package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineStockDto {
    private Long medicineId;
    private Boolean inStock;
    private Integer stockQty;
    private Instant lastUpdatedAt; // can be null if column not added
}
