package com.MediHubAPI.controller;


import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoiceItem;
import com.MediHubAPI.model.billing.InvoicePayment;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;
    private final InvoiceRepository invoiceRepo;

    // Controller

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> upsertInvoiceDraft(
            @Valid @RequestBody InvoiceDtos.CreateInvoiceReq req,
            @RequestHeader(name = "X-User", required = false) String createdBy,
            HttpServletRequest request) {

        Invoice invoice = service.upsertDraft(req, createdBy != null ? createdBy : "system");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        invoice.getId(), // ✅ Convert Long → String
                        request.getRequestURI(),
                        "createInvoiceDraft successful"
                ));

    }


    @PostMapping("/{id}/finalize")
    public InvoiceDtos.FinalizeInvoiceRes finalizeDraftInvoice(@PathVariable Long id) {
        Invoice inv = service.finalizeInvoice(id);
        return new InvoiceDtos.FinalizeInvoiceRes(inv.getId(), inv.getBillNumber(), inv.getIssuedAt(), inv.getStatus().name());
    }


//    @PostMapping("/{id}/payments")
//    public List<InvoicePayment> addPayments(
//            @PathVariable Long id,
//            @Valid @RequestBody InvoiceDtos.AddPaymentsReq req
//    ) {
//        // Call service method to handle batch
//        return service.addPayments(id, req.payments());
//    }


    @PostMapping("/{id}/payments")
    public List<InvoiceDtos.PaymentView> addPayments(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceDtos.AddPaymentsReq req
    ) {
        List<InvoicePayment> saved = service.addPayments(id, req.payments());
        return saved.stream().map(p -> new InvoiceDtos.PaymentView(
                p.getId(),
                p.getIdempotencyKey(),
                p.getMethod().name(),
                p.getAmount(),
                p.getTxnRef(),
                p.getReceiptNo(),
                p.getReceivedAt(),
                p.getReceivedBy(),
                p.getNotes()
        )).toList();
    }

    // Controller
    @GetMapping("/{id}")
    public InvoiceDtos.InvoiceRes get(@PathVariable Long id) {
        var inv = service.get(id); // still entity in service
        var doctorName = inv.getDoctor() != null ? (inv.getDoctor().getFirstName() + " " + inv.getDoctor().getLastName()).trim() : null;
        var patientName = inv.getPatient() != null ? (inv.getPatient().getFirstName() + " " + inv.getPatient().getLastName()).trim() : null;

        return new InvoiceDtos.InvoiceRes(inv.getId(), inv.getBillNumber(), inv.getStatus().name(), doctorName, patientName, inv.getGrandTotal(), inv.getPaidAmount(), inv.getBalanceDue(), inv.getIssuedAt());
    }

    @PostMapping("/{id}/void")
    public void voidInvoice(@PathVariable Long id, @RequestParam(required = false) String reason) {
        service.voidInvoice(id, reason);
    }

     @GetMapping
    public org.springframework.data.domain.Page<InvoiceDtos.InvoiceView> search(@RequestParam(required = false) String status, @RequestParam(required = false) Long doctorId, @RequestParam(required = false) Long patientId, org.springframework.data.domain.Pageable pageable) {
        var page = invoiceRepo.findAll(pageable); // (add Specification filters later)
        return page.map(inv -> {
            String doctorName = inv.getDoctor() == null ? null : ((inv.getDoctor().getFirstName() == null ? "" : inv.getDoctor().getFirstName()) + " " + (inv.getDoctor().getLastName() == null ? "" : inv.getDoctor().getLastName())).trim();

            String patientName = inv.getPatient() == null ? null : ((inv.getPatient().getFirstName() == null ? "" : inv.getPatient().getFirstName()) + " " + (inv.getPatient().getLastName() == null ? "" : inv.getPatient().getLastName())).trim();

            return new InvoiceDtos.InvoiceView(inv.getId(), inv.getBillNumber(), inv.getStatus().name(), doctorName.isBlank() ? null : doctorName, patientName.isBlank() ? null : patientName, inv.getGrandTotal(), inv.getPaidAmount(), inv.getBalanceDue(), inv.getIssuedAt());
        });
    }

//     @GetMapping("/{id}/payments")
//    public Page<InvoiceDtos.PaymentView> listPayments(@PathVariable Long id, @org.springframework.data.web.PageableDefault(size = 20, sort = "receivedAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
//        return service.listPayments(id, pageable);
//    }

    @GetMapping("/all")
    public InvoiceDtos.InvoiceDraftRes getAllInvoicesByAppointment(
            @RequestParam Long appointmentId,
            @RequestParam(required = false) InvoiceDtos.ItemType itemType
    ) {
        Invoice draft = service.getDraftByAppointment(appointmentId);

        var items = draft.getItems().stream()
                .filter(it -> {
                    if (itemType == null) return true;
                    if (it.getItemType() == null) return false;
                    return it.getItemType().name().trim().equalsIgnoreCase(itemType.name());
                })
                .map(InvoiceController::toDraftItem)
                .toList();

        return new InvoiceDtos.InvoiceDraftRes(
                draft.getId(),
                draft.getStatus().name(),
                draft.getDoctor() != null ? draft.getDoctor().getId() : null,
                draft.getPatient() != null ? draft.getPatient().getId() : null,
                draft.getAppointmentId(),
                draft.getClinicId(),                    // never "null" string
                formatTokenNo(draft.getToken()),        // TKN-019
                draft.getQueue(),
                draft.getRoom(),
                draft.getCurrency(),
                draft.getNotes(),
                items,
                draft.getSubTotal(),
                draft.getDiscountTotal(),
                draft.getTaxTotal(),
                draft.getGrandTotal(),
                toUtc(draft.getCreatedAt()),
                toUtc(draft.getUpdatedAt()),
                draft.getVersion()
        );
    }


    private static InvoiceDtos.InvoiceDraftRes.Item toDraftItem(InvoiceItem it) {
        InvoiceDtos.ItemType itemType = (it.getItemType() == null)
                ? null
                : InvoiceDtos.ItemType.valueOf(it.getItemType().name());

        return new InvoiceDtos.InvoiceDraftRes.Item(
                itemType,
                it.getRefId(),
                it.getServiceItemId(),
                it.getCode(),
                it.getName(),
                it.getQty(),
                it.getUnitPrice(),
                it.getDiscountAmount(),
                it.getTaxPercent()
        );
    }

    private static String formatTokenNo(Integer token) {
        return token == null ? null : String.format("TKN-%03d", token);
    }

    private static java.time.OffsetDateTime toUtc(java.time.LocalDateTime dt) {
        return dt == null ? null : dt.atOffset(java.time.ZoneOffset.UTC);
    }
}