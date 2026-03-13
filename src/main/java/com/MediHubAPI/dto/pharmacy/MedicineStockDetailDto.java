package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineStockDetailDto {
    private Long medicineId;
    private String medicineCode;
    private String medicineName;
    private String brand;
    private String form;
    private String composition;
    private Integer availableQty;
    private Integer reservedQty;
    private Integer reorderLevel;
    private LocalDate nearestExpiryDate;
    private BigDecimal stockValue;
    private Boolean lowStock;
}
