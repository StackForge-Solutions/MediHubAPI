// src/main/java/com/MediHubAPI/billing/web/InvoiceController.java
package com.MediHubAPI.controller;


import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoicePayment;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final InvoiceRepository invoiceRepo;

    @PostMapping
    public Invoice create(@Valid @RequestBody InvoiceDtos.CreateInvoiceReq req,
                          @RequestHeader(name = "X-User", required = false) String createdBy) {
        return service.createDraft(req, createdBy != null ? createdBy : "system");
    }

    @PostMapping("/{id}/finalize")
    public InvoiceDtos.FinalizeInvoiceRes finalize(@PathVariable Long id) {
        Invoice inv = service.finalizeInvoice(id);
        return new InvoiceDtos.FinalizeInvoiceRes(inv.getId(), inv.getBillNumber(), inv.getIssuedAt(), inv.getStatus().name());
    }

    @PostMapping("/{id}/payments")
    public InvoicePayment addPayment(@PathVariable Long id, @Valid @RequestBody InvoiceDtos.AddPaymentReq req) {
        return service.addPayment(id, req);
    }

//    @GetMapping("/{id}")
//    public Invoice get(@PathVariable Long id) { return service.get(id); }

    // Controller
    @GetMapping("/{id}")
    public InvoiceDtos.InvoiceRes get(@PathVariable Long id) {
        var inv = service.get(id); // still entity in service
        var doctorName = inv.getDoctor() != null ? (inv.getDoctor().getFirstName() + " " + inv.getDoctor().getLastName()).trim() : null;
        var patientName = inv.getPatient() != null ? (inv.getPatient().getFirstName() + " " + inv.getPatient().getLastName()).trim() : null;

        return new InvoiceDtos.InvoiceRes(
                inv.getId(),
                inv.getBillNumber(),
                inv.getStatus().name(),
                doctorName,
                patientName,
                inv.getGrandTotal(),
                inv.getPaidAmount(),
                inv.getBalanceDue(),
                inv.getIssuedAt()
        );
    }

    @PostMapping("/{id}/void")
    public void voidInvoice(@PathVariable Long id, @RequestParam(required = false) String reason) {
        service.voidInvoice(id, reason);
    }

    // src/main/java/com/MediHubAPI/billing/web/InvoiceController.java
    @GetMapping
    public org.springframework.data.domain.Page<InvoiceDtos.InvoiceView> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            org.springframework.data.domain.Pageable pageable
    ) {
        var page = invoiceRepo.findAll(pageable); // (add Specification filters later)
        return page.map(inv -> {
            String doctorName = inv.getDoctor() == null ? null :
                    ((inv.getDoctor().getFirstName() == null ? "" : inv.getDoctor().getFirstName()) + " " +
                            (inv.getDoctor().getLastName()  == null ? "" : inv.getDoctor().getLastName())).trim();

            String patientName = inv.getPatient() == null ? null :
                    ((inv.getPatient().getFirstName() == null ? "" : inv.getPatient().getFirstName()) + " " +
                            (inv.getPatient().getLastName()  == null ? "" : inv.getPatient().getLastName())).trim();

            return new InvoiceDtos.InvoiceView(
                    inv.getId(),
                    inv.getBillNumber(),
                    inv.getStatus().name(),
                    doctorName.isBlank() ? null : doctorName,
                    patientName.isBlank() ? null : patientName,
                    inv.getGrandTotal(),
                    inv.getPaidAmount(),
                    inv.getBalanceDue(),
                    inv.getIssuedAt()
            );
        });
    }


}
