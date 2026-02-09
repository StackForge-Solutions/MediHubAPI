package com.MediHubAPI.controller.billing;

import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billing")
@Validated
public class BillingDraftController {

    private final InvoiceService invoiceService;

    public BillingDraftController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Helper to create consultation draft if none exists; otherwise returns existing draft/invoice view.
     */
    @PostMapping("/drafts/consultation")
    public ResponseEntity<InvoiceDtos.AppointmentDraftView> consultationDraftHelper(
            @Valid @RequestBody InvoiceDtos.ConsultationDraftHelperReq req,
            @RequestHeader(name = "X-User", required = false) String createdBy
    ) {
        InvoiceDtos.AppointmentDraftView res = invoiceService.createConsultDraftIfMissing(req, createdBy != null ? createdBy : "system");
        return ResponseEntity.ok(res);
    }

    /**
     * Invoice summary by invoiceId.
     */
    @GetMapping("/invoices/{invoiceId}/summary")
    public ResponseEntity<InvoiceDtos.InvoiceSummaryView> invoiceSummary(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoiceSummary(invoiceId));
    }
}
