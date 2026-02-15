package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoicePayment;
import com.MediHubAPI.service.PrescribedTestInvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for drafting and finalizing invoices for Prescribed Tests.
 */
@RestController
@RequestMapping("/api/prescribed-tests/invoices")
@RequiredArgsConstructor
public class PrescribedTestInvoiceController {

    private final PrescribedTestInvoiceService service;

    /**
     * 🧾 Create or update draft invoice for prescribed tests of a VisitSummary.
     */
    @PostMapping("/draft")
    public ResponseEntity<ApiResponse<Long>> upsertDraftInvoice(
            @RequestBody InvoiceDtos.CreateInvoiceReq req,
            @RequestHeader(name = "X-User", required = false) String createdBy,
            HttpServletRequest request
    ) {
        Invoice invoice = service.upsertPrescribedTestDraft(req, createdBy != null ? createdBy : "system");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        invoice.getId(),
                        request.getRequestURI(),
                        "Prescribed Test draft invoice created/updated successfully"
                ));
    }

    /**
     *  Finalize the draft invoice for prescribed tests.
     */
    @PostMapping("/{invoiceId}/finalize")
    public InvoiceDtos.FinalizeInvoiceRes finalizePrescribedTestInvoice(@PathVariable Long invoiceId) {
        Invoice inv = service.finalizePrescribedTestInvoice(invoiceId);
        return new InvoiceDtos.FinalizeInvoiceRes(inv.getId(), inv.getBillNumber(),
                inv.getIssuedAt(), inv.getStatus().name());
    }

    /**
     * 🧾 Add a single payment to a prescribed test invoice.
     */
    @PostMapping("/{invoiceId}")
    public ResponseEntity<ApiResponse<InvoicePayment>> addPayment(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceDtos.AddPaymentReq req,
            @RequestHeader(value = "X-User", required = false) String receivedBy
    ) {
        InvoicePayment payment = service.addPayment(invoiceId, req, receivedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(payment, "/api/prescribed-tests/payments/" + invoiceId,
                        "Payment recorded successfully"));
    }

    /**
     * 💳 Add multiple payments in one request.
     */
    @PostMapping("/{invoiceId}/bulk")
    public ResponseEntity<ApiResponse<List<InvoicePayment>>> addPaymentsBulk(
            @PathVariable Long invoiceId,
            @Valid @RequestBody InvoiceDtos.AddPaymentsReq req,
            @RequestHeader(value = "X-User", required = false) String receivedBy
    ) {
        List<InvoicePayment> payments = service.addPayments(invoiceId, req.payments(), receivedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(payments, "/api/prescribed-tests/payments/" + invoiceId + "/bulk",
                        payments.size() + " payments recorded successfully"));
    }
}
