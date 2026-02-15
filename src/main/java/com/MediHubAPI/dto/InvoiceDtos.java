// src/main/java/com/MediHubAPI/billing/dto/InvoiceDtos.java
package com.MediHubAPI.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

public class InvoiceDtos {

    public enum ItemType {SERVICE, LAB_TEST, MEDICINE, PACKAGE, OTHER}

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
            @NotBlank String currency,

            @jakarta.validation.constraints.NotNull
            ItemType itemType               //  ADD THIS (request-level)

    ) {
    }

    public record ItemReq(
            Long serviceItemId,
            @NotBlank String name,
            @NotNull @Min(1) Integer qty,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal unitPrice,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal discountAmount,
            @NotNull @Digits(integer = 5, fraction = 2) BigDecimal taxPercent,

            //  add these
            Long refId,
            String code
    ) {
    }

    // ---------- Add Payment ----------
    public record AddPaymentReq(
            @NotBlank String idempotencyKey,
            @NotBlank String method, // CASH, CARD, ...
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal amount,
            String txnRef,
            OffsetDateTime receivedAt,
            String receivedBy,
            String notes,


            LocalDateTime paymentDate,   // 🆕 payment date/time
            String cardType              // 🆕 e.g. "VISA", "MASTERCARD", "RUPAY"
    ) {
    }


    public record AddPaymentsReq(
            @NotEmpty @Valid List<InvoiceDtos.AddPaymentReq> payments
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
    ) {
    }


    public record PaymentView(
            Long id,
            String idempotencyKey,
            String method,
            BigDecimal amount,
            String txnRef,
            String receiptNo,          //  add this
            LocalDateTime receivedAt,
            String receivedBy,
            String notes
    ) {
    }
    public record InvoiceDraftRes(
            Long invoiceId,
            String status,                 // "DRAFT"
            Long doctorId,
            Long patientId,
            Long appointmentId,

            String clinicId,               // null allowed
            String tokenNo,                // "TKN-019"
            String queue,
            String room,

            String currency,
            String notes,

            List<Item> items,

            BigDecimal subTotal,
            BigDecimal discountTotal,
            BigDecimal taxTotal,
            BigDecimal grandTotal,

            OffsetDateTime createdAt,      // UTC ISO
            OffsetDateTime updatedAt,      // UTC ISO
            Long version                  // optimistic lock version
    ) {
        public record Item(
                //  identity/type fields
                ItemType itemType,
                Long refId,
                Long serviceItemId,
                String code,

                //  billing fields
                String name,
                Integer qty,
                BigDecimal unitPrice,
                BigDecimal discountAmount,
                BigDecimal taxPercent
        ) {}
    }

    // ---------- Draft Consultation View ----------
    public record DraftConsultationRes(
            Long invoiceId,
            String notes,
            List<DraftConsultationItem> items
    ) {}

    public record DraftConsultationItem(
            String name,
            Integer qty,
            BigDecimal unitPrice,
            BigDecimal discountAmount,
            Long serviceItemId
    ) {}

    // ---------- Consult Billing Draft (Save/Update) ----------
    public record SaveConsultDraftReq(
            Long invoiceId, // null => create, value => update
            @NotNull Long appointmentId,
            @NotNull Long doctorId,
            @NotNull Long patientId,
            @NotBlank String clinicId,
            String token,               // optional string, we will parse to int if numeric
            String queue,
            String room,
            String currency,
            @NotNull ItemType itemType,
            String notes,
            @NotEmpty @Valid List<ConsultDraftItemReq> items
    ) {}

    public record ConsultDraftItemReq(
            Long serviceItemId,
            String serviceCode,
            @NotBlank String name,
            @NotNull @Min(1) Integer qty,
            @NotNull @Digits(integer = 12, fraction = 2) @DecimalMin(value = "0.00") BigDecimal unitPrice,
            @NotNull @Digits(integer = 12, fraction = 2) @DecimalMin(value = "0.00") BigDecimal discountAmount,
            @NotNull @Digits(integer = 5, fraction = 2) @DecimalMin(value = "0.00") BigDecimal taxPercent
    ) {}

    // Helper to create consultation draft with defaults
    public record ConsultationDraftHelperReq(
            @NotNull Long appointmentId,
            @NotNull Long doctorId,
            @NotNull Long patientId,
            @NotBlank String clinicId,
            BigDecimal fee,
            String currency
    ) {}

    // ---------- Appointment draft/invoice view ----------
    public record AppointmentDraftView(
            Long invoiceId,
            String status,
            String tokenNo,
            String patientName,
            String doctorName,
            String department,
            List<AppointmentItem> items,
            AppointmentPayment payment,
            BigDecimal paidTotal,
            BigDecimal balanceDue
    ) {}

    public record AppointmentItem(
            String serviceCode,
            String serviceName,
            BigDecimal unitPrice,
            Integer quantity,
            Boolean taxable,
            BigDecimal taxPercent
    ) {}

    public record AppointmentPayment(
            String mode,
            BigDecimal amountPaid,
            String cardType,
            String bankName,
            String approvalRefNo,
            String txnRefNo
    ) {}

    // ---------- Invoice summary ----------
    public record InvoiceSummaryView(
            Long invoiceId,
            String status,
            BigDecimal paidTotal,
            BigDecimal balanceDue,
            AppointmentPayment payment
    ) {}
}
