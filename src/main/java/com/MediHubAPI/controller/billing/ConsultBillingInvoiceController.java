package com.MediHubAPI.controller.billing;

import com.MediHubAPI.service.InvoiceService;
import com.MediHubAPI.model.billing.Invoice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/consult-billing/invoices")
public class ConsultBillingInvoiceController {

    private final InvoiceService invoiceService;

    public ConsultBillingInvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @PostMapping("/{invoiceId}/finalize")
    public ResponseEntity<Map<String, Object>> finalizeInvoice(
            @PathVariable Long invoiceId,
            @RequestHeader(name = "X-User", required = false) String finalizedBy
    ) {
        Invoice inv = invoiceService.finalizeInvoice(invoiceId);
        return ResponseEntity.ok(Map.of("data", Map.of("status", inv.getStatus().name())));
    }
}
