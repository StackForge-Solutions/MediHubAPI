package com.MediHubAPI.dto.pharmacy;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineSearchItemDto {
    private Long id;
    private String form;          // TAB/CAP/SYR/INJ/...
    private String brand;         // CROCIN
    private String composition;   // Paracetamol 500mg
    private boolean inStock;
    private int stockQty;
}
