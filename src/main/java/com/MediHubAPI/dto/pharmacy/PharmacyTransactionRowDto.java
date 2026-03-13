package com.MediHubAPI.dto.pharmacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyTransactionRowDto {
    private Long transactionId;
    private Instant transactionTime;
    private Long medicineId;
    private String medicineName;
    private Long batchId;
    private String batchNo;
    private Long vendorId;
    private String vendorName;
    private String transactionType;
    private Integer qtyIn;
    private Integer qtyOut;
    private Integer balanceAfter;
    private BigDecimal unitCost;
    private BigDecimal unitPrice;
    private String referenceType;
    private Long referenceId;
    private String referenceNo;
    private String createdBy;
    private String note;
}
