package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineStockTransactionDto {
    private Long transactionId;
    private Instant transactionTime;
    private String transactionType;
    private String batchNo;
    private Integer qtyIn;
    private Integer qtyOut;
    private Integer balanceAfter;
    private String referenceType;
    private Long referenceId;
    private String referenceNo;
    private String note;
}
