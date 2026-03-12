package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManageStockRowDto {
    private Long medicineId;
    private String medicineCode;
    private String medicineName;
    private String brand;
    private String form;
    private Integer availableQty;
    private Integer reservedQty;
    private Integer reorderLevel;
    private BigDecimal sellingPrice;
    private BigDecimal mrp;
    private LocalDate nearestExpiryDate;
    private BigDecimal stockValue;
    private String stockStatus;
    private Boolean lowStock;
    private Boolean inStock;
}
