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

import java.util.Map;


@RestController
@RequestMapping("/api/consult-billing/drafts")
@Validated
public class ConsultBillingController {

    private final InvoiceService invoiceService;

    public ConsultBillingController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /**
     * Save or update a consultation billing draft.
     */
    @PostMapping
    public ResponseEntity<Map<String, Long>> saveDraft(
            @Valid @RequestBody InvoiceDtos.SaveConsultDraftReq req,
            @RequestHeader(name = "X-User", required = false) String createdBy
    ) {
        Long id = invoiceService.saveConsultDraft(req, createdBy != null ? createdBy : "system");
        return ResponseEntity.ok(Map.of("data", id));
    }

    /**
     * Fetch latest draft or finalized invoice for an appointment.
     * Returns status NEW with null invoiceId if none exists.
     */
    @GetMapping("/appointments/{appointmentId}/draft")
    public ResponseEntity<InvoiceDtos.AppointmentDraftView> getDraftForAppointment(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(invoiceService.getAppointmentDraftOrInvoice(appointmentId));
    }



}
