package com.MediHubAPI.dto.pharmacy;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineSearchMeta {
    private String mode;
    private String form;
    private String q;
    private int limit;
    private boolean inStockOnly;
    private int returned;
}
