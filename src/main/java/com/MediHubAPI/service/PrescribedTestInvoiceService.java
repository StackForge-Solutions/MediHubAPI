package com.MediHubAPI.service;

import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.model.PrescribedTest;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoiceItem;
import com.MediHubAPI.model.billing.InvoicePayment;
import com.MediHubAPI.repository.InvoicePaymentRepository;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.repository.PrescribedTestRepository;
import com.MediHubAPI.repository.VisitSummaryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.MediHubAPI.util.MoneyUtil.round;

/**
 * Handles creation and finalization of invoices for PrescribedTests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrescribedTestInvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final PrescribedTestRepository prescribedRepo;
    private final VisitSummaryRepository visitRepo;
    private final BillNumberSequenceService billSeq;
    private final InvoicePaymentRepository paymentRepo;

    @Transactional
    public Invoice upsertPrescribedTestDraft(InvoiceDtos.CreateInvoiceReq req, String createdBy) {
        log.info("🧾 Upserting PrescribedTest Invoice draft for appointmentId={} by {}", req.appointmentId(), createdBy);

        // Step 1️⃣: Validate visit and prescribed tests
        VisitSummary vs = visitRepo.findByAppointmentId(req.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "VisitSummary not found for appointmentId: " + req.appointmentId()));

        List<PrescribedTest> tests = prescribedRepo.findByVisitSummary_Id(vs.getId());
        if (tests.isEmpty()) {
            throw new EntityNotFoundException("No prescribed tests found for visitSummaryId: " + vs.getId());
        }

        // Step 2️⃣: Check for existing DRAFT invoice (managed entity)
        Optional<Invoice> existingOpt =
                invoiceRepo.findFirstByAppointmentIdAndStatus(req.appointmentId(), Invoice.Status.DRAFT);

        Invoice inv;
        boolean isNew = existingOpt.isEmpty();

        if (isNew) {
            // 🆕 Create new draft invoice
            inv = new Invoice();
            inv.setAppointmentId(req.appointmentId());
            inv.setDoctor(vs.getDoctor());
            inv.setPatient(vs.getPatient());
            inv.setClinicId(req.clinicId());
            inv.setCurrency(req.currency());
            inv.setToken(req.token());
            inv.setQueue(req.queue());
            inv.setRoom(req.room());
            inv.setNotes(req.notes());
            inv.setStatus(Invoice.Status.DRAFT);
            inv.setCreatedBy(createdBy);
            inv.setCreatedAt(LocalDateTime.now());
            log.debug("Creating new draft invoice for appointmentId={}", req.appointmentId());
        } else {
            // ✏️ Update existing draft (ensure managed entity)
            Long invoiceId = existingOpt.get().getId();
            inv = invoiceRepo.findById(invoiceId)
                    .orElseThrow(() -> new EntityNotFoundException("Existing draft not found for id=" + invoiceId));
            if (inv.getItems() != null && !inv.getItems().isEmpty()) {
                inv.getItems().clear(); // orphanRemoval-safe
            }
            inv.setNotes(req.notes());
            log.debug("Updating existing draft invoice id={} for appointmentId={}", invoiceId, req.appointmentId());
        }

        // Step 3️⃣: Build invoice items from prescribed tests
        List<InvoiceItem> newItems = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;

        int sl = 1;
        for (PrescribedTest t : tests) {
            BigDecimal price = BigDecimal.valueOf(t.getPrice() != null ? t.getPrice() : 0);
            BigDecimal qty = BigDecimal.valueOf(t.getQuantity() != null ? t.getQuantity() : 1);
            BigDecimal lineTotal = price.multiply(qty);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(inv)
                    .slNo(sl++)
                    .name(t.getName())
                    .qty(qty.intValue())
                    .unitPrice(price)
                    .discountAmount(BigDecimal.ZERO)
                    .taxPercent(BigDecimal.ZERO)
                    .taxAmount(BigDecimal.ZERO)
                    .lineTotal(round(lineTotal))
                    .build();

            newItems.add(item);
            subTotal = subTotal.add(lineTotal);
        }

        inv.getItems().addAll(newItems);
        inv.setSubTotal(round(subTotal));
        inv.setDiscountTotal(BigDecimal.ZERO);
        inv.setTaxTotal(BigDecimal.ZERO);
        inv.setRoundOff(BigDecimal.ZERO);
        inv.setGrandTotal(round(subTotal));

        if (inv.getPaidAmount() == null) {
            inv.setPaidAmount(BigDecimal.ZERO);
        }
        inv.setBalanceDue(inv.getGrandTotal().subtract(inv.getPaidAmount()));

        // Step 4️⃣: Save (UPSERT — INSERT if new, UPDATE if existing)
        try {
            Invoice saved = isNew ? invoiceRepo.save(inv) : invoiceRepo.saveAndFlush(inv);
            log.info("✅ Draft invoice {} successfully {} (GrandTotal: {})",
                    saved.getId(), (isNew ? "created" : "updated"), saved.getGrandTotal());
            return saved;
        } catch (DataIntegrityViolationException e) {
            log.error("⚠️ Duplicate draft detected for appointmentId={}: {}", req.appointmentId(), e.getMessage());
            throw new IllegalStateException(
                    "A draft already exists for this appointment. Please refresh or reuse the existing draft.", e);
        }
    }




    /**
     * Finalizes the prescribed test draft invoice and generates bill number.
     */
    @Transactional
    public Invoice finalizePrescribedTestInvoice(Long invoiceId) {
        Invoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        if (inv.getStatus() != Invoice.Status.DRAFT)
            throw new IllegalStateException("Only DRAFT invoices can be finalized");

        inv.setBillNumber(billSeq.next(inv.getClinicId()));
        inv.setIssuedAt(LocalDateTime.now());
        inv.setStatus(Invoice.Status.ISSUED);

        return invoiceRepo.save(inv);
    }

    /**
     * 💳 Add a single payment to a prescribed test invoice.
     */
    @Transactional
    public InvoicePayment addPayment(Long invoiceId, InvoiceDtos.AddPaymentReq req, String receivedBy) {
        Invoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id: " + invoiceId));

        if (inv.getStatus() == Invoice.Status.VOID)
            throw new IllegalStateException("Cannot make payment for a VOID invoice");

        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        // 1️⃣ Prevent duplicate payments (same txnRef)
        if (req.txnRef() != null &&
                paymentRepo.existsByTxnRefAndInvoiceId(req.txnRef(), invoiceId)) {
            throw new IllegalStateException("Duplicate payment detected: same transaction reference already exists");
        }

        // 2️⃣ Prevent overpayment
        BigDecimal dueBefore = inv.getGrandTotal().subtract(inv.getPaidAmount());
        if (req.amount().compareTo(dueBefore) > 0) {
            throw new IllegalArgumentException(
                    String.format("Payment amount %.2f exceeds remaining balance %.2f",
                            req.amount(), dueBefore));
        }

        InvoicePayment.Method method = InvoicePayment.Method.valueOf(req.method());
        InvoicePayment payment = InvoicePayment.builder()
                .invoice(inv)
                .method(method)
                .amount(req.amount())
                .txnRef(req.txnRef())
                .receivedAt(req.receivedAt() != null
                        ? req.receivedAt().toLocalDateTime()
                        : LocalDateTime.now())
                .paymentDate(req.paymentDate() != null
                        ? req.paymentDate()
                        : LocalDateTime.now()) // 🆕 Default current time
                .cardType(req.cardType())      // 🆕 Optional
                .receivedBy(receivedBy != null ? receivedBy : req.receivedBy())
                .notes(req.notes())
                .build();

        paymentRepo.save(payment);

        // 3️⃣ Update totals safely
        BigDecimal updatedPaid = inv.getPaidAmount().add(payment.getAmount());
        inv.setPaidAmount(round(updatedPaid));

        BigDecimal newBalance = inv.getGrandTotal().subtract(inv.getPaidAmount());
        if (newBalance.signum() <= 0) {
            inv.setBalanceDue(BigDecimal.ZERO);
            inv.setStatus(Invoice.Status.PAID);
        } else {
            inv.setBalanceDue(round(newBalance));
            inv.setStatus(Invoice.Status.PARTIALLY_PAID);
        }

        invoiceRepo.save(inv);

        log.info("✅ Payment of {} recorded for invoice={}, method={}, balanceDue={}",
                payment.getAmount(), invoiceId, payment.getMethod(), inv.getBalanceDue());

        return payment;
    }


    /**
     * 💳 Add multiple payments in one transaction (bulk).
     */
    @Transactional
    public List<InvoicePayment> addPayments(Long invoiceId, List<InvoiceDtos.AddPaymentReq> paymentsReq, String receivedBy) {
        List<InvoicePayment> added = new ArrayList<>();
        for (InvoiceDtos.AddPaymentReq req : paymentsReq) {
            added.add(addPayment(invoiceId, req, receivedBy));
        }
        return added;
    }
}
