// src/main/java/com/MediHubAPI/billing/dto/InvoiceDtos.java
package com.MediHubAPI.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class InvoiceDtos {

    // ---------- Create Draft ----------
    public record CreateInvoiceReq(
            @NotNull Long doctorId,
            @NotNull Long patientId,
            Long appointmentId,
            String clinicId,
            Integer token,
            String queue,
            String room,
            String notes,
            @NotEmpty List<ItemReq> items,
            @NotBlank String currency
    ) {
    }

    public record ItemReq(
            Long serviceItemId,
            @NotBlank String name,
            @NotNull @Min(1) Integer qty,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal discountAmount,
            @NotNull @Digits(integer = 5, fraction = 2) BigDecimal taxPercent
    ) {
    }

    public record InvoiceSummaryRes(
            Long id, String status, String billNumber,
            BigDecimal grandTotal, BigDecimal paidAmount, BigDecimal balanceDue
    ) {
    }

    // ---------- Add Payment ----------
    public record AddPaymentReq(
            @NotBlank String method, // CASH, CARD, ...
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal amount,
            String txnRef,
            OffsetDateTime receivedAt,
            String receivedBy,
            String notes
    ) {
    }

    // ---------- Finalize ----------
    public record FinalizeInvoiceRes(
            Long id, String billNumber, LocalDateTime issuedAt, String status
    ) {
    }

    public record InvoiceRes(
            Long id, String billNumber, String status,
            String doctorName, String patientName,
            java.math.BigDecimal grandTotal, java.math.BigDecimal paidAmount, java.math.BigDecimal balanceDue,
            LocalDateTime issuedAt
    ) {
    }


    public record InvoiceView(
            Long id,
            String billNumber,
            String status,
            String doctorName,
            String patientName,
            BigDecimal grandTotal,
            BigDecimal paidAmount,
            BigDecimal balanceDue,
            LocalDateTime issuedAt
    ) {}
}
