// src/main/java/com/MediHubAPI/billing/service/InvoiceService.java
package com.MediHubAPI.service;


import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.billing.DoctorServiceItem;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoiceItem;
import com.MediHubAPI.repository.DoctorServiceItemRepository;
import com.MediHubAPI.repository.InvoiceItemRepository;
import com.MediHubAPI.repository.InvoicePaymentRepository;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.model.billing.InvoicePayment;
import com.MediHubAPI.service.billing.BillNumberSequenceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.MediHubAPI.util.MoneyUtil.round;


@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final InvoiceItemRepository itemRepo;
    private final InvoicePaymentRepository payRepo;
    private final DoctorServiceItemRepository serviceRepo;
    private final BillNumberSequenceService billSeq;

    // TODO: inject real UserService to load User entities
    private User loadUser(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }


    /**
     * 🧾 Create or update (upsert) a draft invoice.
     * If a draft already exists for the same appointment, it will be updated.
     */
    /**
     * 🧾 Create or update (upsert) a draft invoice.
     * - Creates a new draft if none exists for the appointment.
     * - Updates existing draft items & totals otherwise.
     */
    @Transactional
    public Invoice upsertDraft(InvoiceDtos.CreateInvoiceReq req, String createdBy) {

        // 1️⃣ Try to find existing draft for same appointment (managed entity)
        Invoice inv = invoiceRepo.findFirstByAppointmentIdAndStatusIn(
                req.appointmentId(), Collections.singleton(Invoice.Status.DRAFT)
        ).orElse(null);

        boolean isNew = (inv == null);

        if (isNew) {
            // 🆕 New invoice (no conflict)
            inv = new Invoice();
            inv.setDoctor(loadUser(req.doctorId()));
            inv.setPatient(loadUser(req.patientId()));
            inv.setAppointmentId(req.appointmentId());
            inv.setClinicId(req.clinicId());
            inv.setCurrency(req.currency());
            inv.setToken(req.token());
            inv.setQueue(req.queue());
            inv.setRoom(req.room());
            inv.setNotes(req.notes());
            inv.setStatus(Invoice.Status.DRAFT);
            inv.setCreatedBy(createdBy);
            inv.setCreatedAt(LocalDateTime.now());
        } else {
            // ✏️ Existing draft — ensure it’s managed
            inv = invoiceRepo.findById(inv.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found for update"));

            inv.setNotes(req.notes());
//            inv.setUpdatedBy(createdBy);
//            inv.setUpdatedAt(LocalDateTime.now());

            // ✅ Hibernate will auto-delete old items (orphanRemoval)
            inv.getItems().clear();
        }

        // 2️⃣ Build new items list
        List<InvoiceItem> newItems = new ArrayList<>();
        int sl = 1;
        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;

        for (InvoiceDtos.ItemReq it : req.items()) {
            String name = it.name();
            if (it.serviceItemId() != null) {
                DoctorServiceItem dsi = serviceRepo.findById(it.serviceItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Service not found"));
                name = dsi.getName();
            }

            BigDecimal qty = BigDecimal.valueOf(it.qty());
            BigDecimal lineBase = it.unitPrice().multiply(qty);
            BigDecimal disc = it.discountAmount();
            BigDecimal taxable = lineBase.subtract(disc);
            BigDecimal tax = taxable.multiply(it.taxPercent()).divide(BigDecimal.valueOf(100));
            BigDecimal lineTotal = taxable.add(tax);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(inv)
                    .serviceItemId(it.serviceItemId())
                    .slNo(sl++)
                    .name(name)
                    .qty(it.qty())
                    .unitPrice(it.unitPrice())
                    .discountAmount(round(disc))
                    .taxPercent(it.taxPercent())
                    .taxAmount(round(tax))
                    .lineTotal(round(lineTotal))
                    .itemType(req.itemType())      // ✅ ADD THIS LINE
                    .build();

            newItems.add(item);
            subTotal = subTotal.add(lineBase);
            discountTotal = discountTotal.add(disc);
            taxTotal = taxTotal.add(tax);
        }

        // 3️⃣ Assign items and totals
        inv.getItems().addAll(newItems);
        inv.setSubTotal(round(subTotal));
        inv.setDiscountTotal(round(discountTotal));
        inv.setTaxTotal(round(taxTotal));
        inv.setRoundOff(BigDecimal.ZERO);

        BigDecimal grand = subTotal.subtract(discountTotal).add(taxTotal);
        inv.setGrandTotal(round(grand));

        if (isNew) {
            inv.setPaidAmount(BigDecimal.ZERO);
            inv.setBalanceDue(inv.getGrandTotal());
        } else {
            BigDecimal alreadyPaid = inv.getPaidAmount() == null ? BigDecimal.ZERO : inv.getPaidAmount();
            inv.setBalanceDue(inv.getGrandTotal().subtract(alreadyPaid));
        }

        // 4️⃣ Save (UPDATE if existing, INSERT if new)
        if (isNew) {
            return invoiceRepo.save(inv); // insert
        } else {
            // ✅ ensure Hibernate treats this as update
            return invoiceRepo.saveAndFlush(inv); // update managed entity
        }
    }




    @Transactional
    public Invoice finalizeInvoice(Long invoiceId) {
        Invoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        if (inv.getStatus() != Invoice.Status.DRAFT)
            throw new IllegalStateException("Only DRAFT can be finalized");

        // generate & save; retry if unique collision
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                inv.setBillNumber(billSeq.next(inv.getClinicId()));
                inv.setIssuedAt(LocalDateTime.now());

                if (inv.getPaidAmount().compareTo(inv.getGrandTotal()) >= 0) {
                    inv.setStatus(Invoice.Status.PAID);
                    inv.setBalanceDue(BigDecimal.ZERO);
                } else {
                    inv.setStatus(Invoice.Status.ISSUED);
                    inv.setBalanceDue(inv.getGrandTotal().subtract(inv.getPaidAmount()));
                }

                return invoiceRepo.save(inv);

            } catch (DataIntegrityViolationException ex) {
                if (attempt == 3) throw ex;
                // retry with a fresh number
            }
        }
        throw new IllegalStateException("Finalize failed unexpectedly");
    }




    @Transactional
    public InvoicePayment addPayment(Long invoiceId, InvoiceDtos.AddPaymentReq req) {
        Invoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        if (inv.getStatus() == Invoice.Status.VOID)
            throw new IllegalStateException("Cannot pay a void invoice");

        InvoicePayment.Method m = InvoicePayment.Method.valueOf(req.method());
        InvoicePayment p = InvoicePayment.builder()
                .invoice(inv)
                .method(m)
                .amount(req.amount())
                .txnRef(req.txnRef())
                .receivedAt(req.receivedAt() != null
                        ? req.receivedAt().toLocalDateTime()
                        : LocalDateTime.now())

                .receivedBy(req.receivedBy())
                .notes(req.notes())
                .build();
        payRepo.save(p);

        inv.setPaidAmount(round(inv.getPaidAmount().add(p.getAmount())));
        BigDecimal due = inv.getGrandTotal().subtract(inv.getPaidAmount());
        if (due.signum() <= 0) {
            inv.setStatus(Invoice.Status.PAID);
            inv.setBalanceDue(BigDecimal.ZERO);
        } else {
            inv.setStatus(Invoice.Status.PARTIALLY_PAID);
            inv.setBalanceDue(round(due));
        }
        invoiceRepo.save(inv);
        return p;
    }

    @Transactional(readOnly = true)
    public Invoice get(Long id) {
        return invoiceRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    @Transactional
    public void voidInvoice(Long id, String reason) {
        Invoice inv = get(id);
        if (inv.getStatus() == Invoice.Status.PAID)
            throw new IllegalStateException("Use refund/credit note instead of void for PAID");
        inv.setStatus(Invoice.Status.VOID);
        invoiceRepo.save(inv);
        // TODO: persist reason in audit table
    }

    // src/main/java/com/MediHubAPI/billing/service/InvoiceService.java
    @Transactional(readOnly = true)
    public Page<InvoiceDtos.PaymentView> listPayments(Long invoiceId, Pageable pageable) {
        // Optional: verify invoice exists (prevents leaking info if id is wrong)
        invoiceRepo.findById(invoiceId).orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        return payRepo.findByInvoiceId(invoiceId, pageable)
                .map(p -> new InvoiceDtos.PaymentView(
                        p.getId(),
                        p.getMethod().name(),
                        p.getAmount(),
                        p.getTxnRef(),
                        p.getReceivedAt(),
                        p.getReceivedBy(),
                        p.getNotes()
                ));
    }

    @Transactional(readOnly = true)
    public Invoice getDraftByAppointment(Long appointmentId) {
        return invoiceRepo.findTopByAppointmentIdOrderByCreatedAtDesc(
                appointmentId
        ).orElseThrow(() -> new EntityNotFoundException("No DRAFT/PAID invoice for appointmentId=" + appointmentId));
    }

    @Transactional
    public List<InvoicePayment> addPayments(Long invoiceId, List<InvoiceDtos.AddPaymentReq> payments) {
        List<InvoicePayment> addedPayments = new ArrayList<>();

        for (InvoiceDtos.AddPaymentReq p : payments) {
            InvoicePayment payment = addPayment(invoiceId, p); // reuse existing single payment logic
            addedPayments.add(payment);
        }

        return addedPayments;
    }

}
