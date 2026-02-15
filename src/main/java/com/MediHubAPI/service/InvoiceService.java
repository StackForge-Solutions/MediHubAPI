package com.MediHubAPI.service;

import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.exception.ValidationException;
import com.MediHubAPI.exception.ValidationException.ValidationErrorDetail;
import com.MediHubAPI.exception.billing.DraftUpsertNotAllowedException;
import com.MediHubAPI.exception.billing.IdempotencyConflictException;
import com.MediHubAPI.exception.billing.InvoiceNotFoundException;
import com.MediHubAPI.exception.HospitalAPIException;
import com.MediHubAPI.model.User;
import com.MediHubAPI.model.billing.DoctorServiceItem;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoiceAuditLog;
import com.MediHubAPI.model.billing.InvoiceItem;
import com.MediHubAPI.model.billing.InvoicePayment;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.repository.DoctorServiceItemRepository;
import com.MediHubAPI.repository.InvoicePaymentRepository;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.repository.AppointmentRepository;
import com.MediHubAPI.service.billing.BillNumberSequenceService;
import com.MediHubAPI.service.billing.InvoiceAuditService;
import com.MediHubAPI.service.billing.ReceiptNumberSequenceService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static com.MediHubAPI.util.MoneyUtil.round;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepo;
    private final InvoicePaymentRepository payRepo;
    private final DoctorServiceItemRepository serviceRepo;
    private final BillNumberSequenceService billSeq;
    private final ReceiptNumberSequenceService receiptSeq;
    private final AppointmentRepository appointmentRepo;

    // ✅ NEW
    private final InvoiceAuditService auditService;

    private static final EnumSet<InvoicePayment.Method> GATEWAY_METHODS =
            EnumSet.of(InvoicePayment.Method.UPI, InvoicePayment.Method.CARD, InvoicePayment.Method.NETBANKING);

    private static final EnumSet<InvoicePayment.Method> RECEIPT_METHODS =
            EnumSet.of(InvoicePayment.Method.CASH, InvoicePayment.Method.OTHER);

    // TODO: inject real UserService to load User entities
    private User loadUser(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    /*
      ✅ FIXED: Upsert draft with strict rules:
      - Lock latest invoice row for appointment (if exists)
      - If latest status is DRAFT => update it
      - If latest status is ISSUED/PARTIALLY_PAID/PAID => 409 reject (no draft update allowed)
      - If no invoice exists => create new DRAFT

      ✅ PaidAmount is never reset by draft upsert.
      ✅ Concurrency safe when combined with DB unique constraint (see SQL section).
     */

    /**
     * ✅ Draft Upsert (HARD RULE):
     * - appointmentId এ যদি latest invoice status ISSUED/PARTIALLY_PAID/PAID হয় -> 409 throw
     * - only DRAFT থাকলে update allowed
     * - none থাকলে new DRAFT create

     * ✅ Concurrency safe:
     * - appointmentId ভিত্তিতে latest invoice row কে PESSIMISTIC lock দিয়ে পড়ছি
     *   (findLatestByAppointmentIdForUpdate + PageRequest(0,1))
     */
    @Transactional
    public Invoice upsertDraft(InvoiceDtos.CreateInvoiceReq req, String createdBy) {

        // 0) Lock latest invoice row (if exists) for this appointment
        Pageable top1 = PageRequest.of(0, 1);
        List<Invoice> latestList = invoiceRepo.findLatestByAppointmentIdForUpdate(req.appointmentId(),req.queue(),top1);
        Invoice latest = latestList.isEmpty() ? null : latestList.get(0);

        // 1) If latest is non-draft active state -> BLOCK draft upsert
        if (latest != null) {
            Invoice.Status st = latest.getStatus();
            if (st == Invoice.Status.ISSUED || st == Invoice.Status.PARTIALLY_PAID || st == Invoice.Status.PAID) {
                throw new DraftUpsertNotAllowedException(
                        "Invoice already issued/partially paid. Draft update not allowed.",
                        latest.getId(),
                        latest.getStatus()
                );
            }
            // Optional: if VOID -> treat as no active invoice (allow new draft)
            // If latest is DRAFT -> we update that draft
        }

        // 2) Determine target draft invoice (update existing DRAFT if present, else create new)
        Invoice inv;
        boolean isNew = (latest == null || latest.getStatus() != Invoice.Status.DRAFT);

        if (isNew) {
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
            // latest is DRAFT
            inv = invoiceRepo.findById(latest.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found for update"));

            // update allowed fields
            inv.setNotes(req.notes());

            // replace items cleanly
            inv.getItems().clear();
        }

        // 3) Build items + totals
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
            BigDecimal disc = it.discountAmount() == null ? BigDecimal.ZERO : it.discountAmount();

            BigDecimal taxable = lineBase.subtract(disc);
            BigDecimal taxPercent = it.taxPercent() == null ? BigDecimal.ZERO : it.taxPercent();
             BigDecimal tax = taxable.multiply(taxPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal lineTotal = taxable.add(tax);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(inv)
                    .serviceItemId(it.serviceItemId())
                    .slNo(sl++)
                    .name(name)
                    .qty(it.qty())
                    .unitPrice(it.unitPrice())
                    .discountAmount(round(disc))
                    .taxPercent(round(taxPercent))
                    .taxAmount(round(tax))
                    .lineTotal(round(lineTotal))
                    .itemType(req.itemType())          // uses CreateInvoiceReq.itemType
                    // .refId(...)  // only if ItemReq has it (you had compile issue before)
                    // .code(...)   // only if ItemReq has it
                    .build();

            newItems.add(item);

            subTotal = subTotal.add(lineBase);
            discountTotal = discountTotal.add(disc);
            taxTotal = taxTotal.add(tax);
        }

        inv.getItems().addAll(newItems);

        inv.setSubTotal(round(subTotal));
        inv.setDiscountTotal(round(discountTotal));
        inv.setTaxTotal(round(taxTotal));
        inv.setRoundOff(BigDecimal.ZERO);

        BigDecimal grand = subTotal.subtract(discountTotal).add(taxTotal);
        inv.setGrandTotal(round(grand));

        // 4) paidAmount must NEVER reset on draft update
        if (isNew) {
            if (inv.getPaidAmount() == null) inv.setPaidAmount(BigDecimal.ZERO);
            inv.setBalanceDue(round(inv.getGrandTotal().subtract(inv.getPaidAmount())));
        } else {
            BigDecimal alreadyPaid = inv.getPaidAmount() == null ? BigDecimal.ZERO : inv.getPaidAmount();
            inv.setPaidAmount(round(alreadyPaid)); // explicit
            inv.setBalanceDue(round(inv.getGrandTotal().subtract(alreadyPaid)));
        }

        // 5) Save
        if (isNew) return invoiceRepo.save(inv);
        return invoiceRepo.saveAndFlush(inv);
    }

    @Transactional
    public Invoice finalizeInvoice(Long invoiceId) {
        Invoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        if (inv.getStatus() != Invoice.Status.DRAFT)
            throw new IllegalStateException("Only DRAFT can be finalized");

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

                Invoice saved = invoiceRepo.save(inv);

                auditService.log(
                        saved.getId(),
                        saved.getAppointmentId(),
                        InvoiceAuditLog.Action.FINALIZED,
                        "system",
                        "Finalized with status=" + saved.getStatus().name()
                );

                return saved;

            } catch (DataIntegrityViolationException ex) {
                if (attempt == 3) throw ex;
            }
        }
        throw new IllegalStateException("Finalize failed unexpectedly");
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

        auditService.log(
                inv.getId(),
                inv.getAppointmentId(),
                InvoiceAuditLog.Action.VOIDED,
                "system",
                reason
        );
    }

    @Transactional(readOnly = true)
    public Invoice getDraftByAppointment(Long appointmentId) {
        return invoiceRepo.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId)
                .orElseThrow(() -> new EntityNotFoundException("No invoice for appointmentId=" + appointmentId));
    }

    @Transactional
    public Long saveConsultDraft(InvoiceDtos.SaveConsultDraftReq req, String createdBy) {
        List<ValidationErrorDetail> details = new ArrayList<>();

        String currency = (req.currency() == null || req.currency().isBlank()) ? "INR" : req.currency().trim();

        Integer token = null;
        if (req.token() != null && !req.token().isBlank()) {
            try {
                token = Integer.parseInt(req.token().trim());
            } catch (NumberFormatException nfe) {
                details.add(new ValidationErrorDetail("token", "must be numeric"));
            }
        }

        Invoice inv;
        boolean isUpdate = req.invoiceId() != null;
        if (isUpdate) {
            inv = invoiceRepo.findByIdForUpdate(req.invoiceId())
                    .orElseThrow(() -> new InvoiceNotFoundException(req.invoiceId(), true));

            if (inv.getStatus() != Invoice.Status.DRAFT) {
                details.add(new ValidationErrorDetail("invoiceId", "only DRAFT invoices can be updated"));
            }
            if (!Objects.equals(inv.getAppointmentId(), req.appointmentId())) {
                details.add(new ValidationErrorDetail("invoiceId", "appointmentId mismatch"));
            }

            inv.getItems().clear();
        } else {
            inv = new Invoice();
            inv.setStatus(Invoice.Status.DRAFT);
            inv.setCreatedBy(createdBy);
            inv.setCreatedAt(LocalDateTime.now());
            inv.setPaidAmount(BigDecimal.ZERO);
        }

        inv.setAppointmentId(req.appointmentId());
        inv.setDoctor(loadUser(req.doctorId()));
        inv.setPatient(loadUser(req.patientId()));
        inv.setClinicId(req.clinicId());
        inv.setToken(token);
        inv.setQueue(req.queue());
        inv.setRoom(req.room());
        inv.setCurrency(currency);
        inv.setNotes(req.notes());

        BigDecimal subTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal taxTotal = BigDecimal.ZERO;
        List<InvoiceItem> newItems = new ArrayList<>();
        int sl = 1;

        List<InvoiceDtos.ConsultDraftItemReq> itemReqs = req.items();
        if (itemReqs == null || itemReqs.isEmpty()) {
            details.add(new ValidationErrorDetail("items", "must not be empty"));
        } else {
            for (int i = 0; i < itemReqs.size(); i++) {
                InvoiceDtos.ConsultDraftItemReq it = itemReqs.get(i);
                String idx = "items[" + i + "]";

                BigDecimal qty = it.qty() == null ? null : BigDecimal.valueOf(it.qty());
                if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                    details.add(new ValidationErrorDetail(idx + ".qty", "must be greater than 0"));
                    continue;
                }

                BigDecimal unitPrice = it.unitPrice();
                BigDecimal disc = it.discountAmount();
                BigDecimal taxPercent = it.taxPercent();

                BigDecimal lineBase = unitPrice.multiply(qty);
                BigDecimal taxable = lineBase.subtract(disc);
                if (taxable.compareTo(BigDecimal.ZERO) < 0) {
                    details.add(new ValidationErrorDetail(idx + ".discountAmount", "discount exceeds line amount"));
                    taxable = BigDecimal.ZERO;
                }

                BigDecimal tax = taxable.multiply(taxPercent)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal lineTotal = taxable.add(tax);

                subTotal = subTotal.add(lineBase);
                discountTotal = discountTotal.add(disc);
                taxTotal = taxTotal.add(tax);

                InvoiceItem item = InvoiceItem.builder()
                        .invoice(inv)
                        .serviceItemId(it.serviceItemId())
                        .slNo(sl++)
                        .name(it.name())
                        .qty(it.qty())
                        .unitPrice(round(unitPrice))
                        .discountAmount(round(disc))
                        .taxPercent(round(taxPercent))
                        .taxAmount(round(tax))
                        .lineTotal(round(lineTotal))
                        .itemType(req.itemType())
                        .code(it.serviceCode())
                        .build();

                newItems.add(item);
            }
        }

        BigDecimal net = subTotal.subtract(discountTotal);
        if (net.compareTo(BigDecimal.ZERO) <= 0) {
            details.add(new ValidationErrorDetail("items", "total after discounts must be greater than 0"));
        }

        if (!details.isEmpty()) {
            throw new ValidationException("Validation failed", details);
        }

        inv.getItems().addAll(newItems);

        inv.setSubTotal(round(subTotal));
        inv.setDiscountTotal(round(discountTotal));
        inv.setTaxTotal(round(taxTotal));
        inv.setRoundOff(BigDecimal.ZERO);

        BigDecimal grand = subTotal.subtract(discountTotal).add(taxTotal);
        inv.setGrandTotal(round(grand));

        if (inv.getPaidAmount() == null) inv.setPaidAmount(BigDecimal.ZERO);
        inv.setBalanceDue(round(inv.getGrandTotal().subtract(inv.getPaidAmount())));

        Invoice saved = isUpdate ? invoiceRepo.saveAndFlush(inv) : invoiceRepo.save(inv);
        return saved.getId();
    }

    @Transactional
    public InvoiceDtos.AppointmentDraftView createConsultDraftIfMissing(InvoiceDtos.ConsultationDraftHelperReq req, String createdBy) {

        Optional<Invoice> existing = invoiceRepo.findTopByAppointmentIdOrderByCreatedAtDesc(req.appointmentId());
        if (existing.isPresent()) {
            return getAppointmentDraftOrInvoice(req.appointmentId());
        }

        List<ValidationErrorDetail> details = new ArrayList<>();
        if (req.currency() == null || req.currency().isBlank()) {
            details.add(new ValidationErrorDetail("currency", "must not be blank"));
        }
        if (!details.isEmpty()) throw new ValidationException("Validation failed", details);

        BigDecimal fee = req.fee() != null ? req.fee() : BigDecimal.ZERO;

        Invoice inv = new Invoice();
        inv.setStatus(Invoice.Status.DRAFT);
        inv.setCreatedBy(createdBy);
        inv.setCreatedAt(LocalDateTime.now());
        inv.setAppointmentId(req.appointmentId());
        inv.setDoctor(loadUser(req.doctorId()));
        inv.setPatient(loadUser(req.patientId()));
        inv.setClinicId(req.clinicId());
        inv.setCurrency(req.currency().trim());
        inv.setPaidAmount(BigDecimal.ZERO);

        InvoiceItem item = InvoiceItem.builder()
                .invoice(inv)
                .slNo(1)
                .name("Consultation")
                .qty(1)
                .unitPrice(round(fee))
                .discountAmount(BigDecimal.ZERO)
                .taxPercent(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .lineTotal(round(fee))
                .itemType(InvoiceDtos.ItemType.SERVICE)
                .code("CONSULT")
                .build();

        inv.getItems().add(item);

        inv.setSubTotal(round(fee));
        inv.setDiscountTotal(BigDecimal.ZERO);
        inv.setTaxTotal(BigDecimal.ZERO);
        inv.setRoundOff(BigDecimal.ZERO);
        inv.setGrandTotal(round(fee));
        inv.setBalanceDue(round(fee));

        invoiceRepo.save(inv);
        return getAppointmentDraftOrInvoice(req.appointmentId());
    }

    @Transactional(readOnly = true)
    public InvoiceDtos.InvoiceSummaryView getInvoiceSummary(Long invoiceId) {
        Invoice inv = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId, true));

        InvoicePayment latestPay = (inv.getPayments() == null || inv.getPayments().isEmpty()) ? null
                : inv.getPayments().stream()
                .max(Comparator.comparing(InvoicePayment::getReceivedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        InvoiceDtos.AppointmentPayment payment = latestPay == null
                ? defaultPayment()
                : new InvoiceDtos.AppointmentPayment(
                latestPay.getMethod() != null ? latestPay.getMethod().name() : "Cash",
                latestPay.getAmount(),
                latestPay.getCardType(),
                null,
                null,
                latestPay.getTxnRef()
        );

        return new InvoiceDtos.InvoiceSummaryView(
                inv.getId(),
                inv.getStatus() == null ? "ERROR" : inv.getStatus().name(),
                inv.getPaidAmount(),
                inv.getBalanceDue(),
                payment
        );
    }

    @Transactional(readOnly = true)
    public InvoiceDtos.AppointmentDraftView getAppointmentDraftOrInvoice(Long appointmentId) {
        Appointment appt = appointmentRepo.findById(appointmentId)
                .orElseThrow(() -> new HospitalAPIException(org.springframework.http.HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND", "Appointment not found"));

        Optional<Invoice> optInv = invoiceRepo.findTopByAppointmentIdAndItems_ItemTypeOrderByCreatedAtDesc(
                appointmentId,
                InvoiceDtos.ItemType.SERVICE
        );
        if (optInv.isEmpty()) {
            return new InvoiceDtos.AppointmentDraftView(
                    null,
                    "NEW",
                    null,
                    fullName(appt.getPatient()),
                    fullName(appt.getDoctor()),
                    department(appt.getDoctor()),
                    List.of(),
                    defaultPayment(),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }

        Invoice inv = optInv.get();
        String status = inv.getStatus() != null ? inv.getStatus().name() : "ERROR";

        List<InvoiceDtos.AppointmentItem> items = inv.getItems() == null ? List.of() :
                inv.getItems().stream().map(it -> new InvoiceDtos.AppointmentItem(
                        it.getCode(),
                        it.getName(),
                        it.getUnitPrice(),
                        it.getQty(),
                        it.getTaxPercent() != null && it.getTaxPercent().compareTo(BigDecimal.ZERO) > 0,
                        it.getTaxPercent()
                )).toList();

        InvoicePayment latestPay = (inv.getPayments() == null || inv.getPayments().isEmpty()) ? null
                : inv.getPayments().stream()
                .max(Comparator.comparing(InvoicePayment::getReceivedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

        InvoiceDtos.AppointmentPayment payment = latestPay == null
                ? defaultPayment()
                : new InvoiceDtos.AppointmentPayment(
                        latestPay.getMethod() != null ? latestPay.getMethod().name() : "Cash",
                        latestPay.getAmount(),
                        latestPay.getCardType(),
                        null,
                        null,
                        latestPay.getTxnRef()
        );

        return new InvoiceDtos.AppointmentDraftView(
                inv.getId(),
                status,
                inv.getToken() == null ? null : String.valueOf(inv.getToken()),
                fullName(inv.getPatient()),
                fullName(inv.getDoctor()),
                department(inv.getDoctor()),
                items,
                payment,
                inv.getPaidAmount(),
                inv.getBalanceDue()
        );
    }

    private InvoiceDtos.AppointmentPayment defaultPayment() {
        return new InvoiceDtos.AppointmentPayment("Cash", BigDecimal.ZERO, null, null, null, null);
    }

    private String fullName(User u) {
        if (u == null) return null;
        String first = u.getFirstName() == null ? "" : u.getFirstName();
        String last = u.getLastName() == null ? "" : u.getLastName();
        String title = u.getTitle() == null ? "" : u.getTitle();
        String name = (title + " " + first + " " + last).trim();
        return name.isEmpty() ? null : name;
    }

    private String department(User doctor) {
        if (doctor == null || doctor.getSpecialization() == null) return null;
        if (doctor.getSpecialization().getDepartment() != null && !doctor.getSpecialization().getDepartment().isBlank()) {
            return doctor.getSpecialization().getDepartment();
        }
        return doctor.getSpecialization().getName();
    }

    @Transactional
    public InvoicePayment addPaymentRobust(Long invoiceId, InvoiceDtos.AddPaymentReq req) {

        String idemKey = normalize(req.idempotencyKey());
        InvoicePayment.Method method = InvoicePayment.Method.valueOf(normalize(req.method()).toUpperCase());

        Optional<InvoicePayment> byKey = payRepo.findByInvoiceIdAndIdempotencyKey(invoiceId, idemKey);
        if (byKey.isPresent()) {
            validateSamePayloadOrThrow(byKey.get(), invoiceId, method, req);
            return byKey.get();
        }

        Invoice inv = invoiceRepo.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));

        if (inv.getStatus() == Invoice.Status.VOID) {
            throw new IllegalStateException("Cannot pay a VOID invoice");
        }

        String txnRef = normalizeNullable(req.txnRef());
        if (GATEWAY_METHODS.contains(method)) {
            if (txnRef == null) throw new IllegalArgumentException("txnRef is required for " + method);
            Optional<InvoicePayment> existingByTxn = payRepo.findByMethodAndTxnRef(method, txnRef);
            if (existingByTxn.isPresent()) return existingByTxn.get();
        }

        String receiptNo = null;
        if (RECEIPT_METHODS.contains(method)) {
            String clinicId = inv.getClinicId() == null ? "CLINIC" : inv.getClinicId().trim();
            receiptNo = receiptSeq.nextReceiptNo(clinicId, inv.getId());
        }

        LocalDateTime receivedAt = (req.receivedAt() != null)
                ? req.receivedAt().toLocalDateTime()
                : LocalDateTime.now();

        InvoicePayment p = InvoicePayment.builder()
                .invoice(inv)
                .idempotencyKey(idemKey)
                .method(method)
                .amount(req.amount())
                .txnRef(txnRef)
                .receiptNo(receiptNo)
                .receivedAt(receivedAt)
                .receivedBy(req.receivedBy())
                .notes(req.notes())
                .paymentDate(req.paymentDate())
                .cardType(req.cardType())
                .build();

        try {
            payRepo.saveAndFlush(p);
        } catch (DataIntegrityViolationException dup) {
            Optional<InvoicePayment> again = payRepo.findByInvoiceIdAndIdempotencyKey(invoiceId, idemKey);
            if (again.isPresent()) {
                validateSamePayloadOrThrow(again.get(), invoiceId, method, req);
                return again.get();
            }
            if (txnRef != null && GATEWAY_METHODS.contains(method)) {
                Optional<InvoicePayment> exTxn = payRepo.findByMethodAndTxnRef(method, txnRef);
                if (exTxn.isPresent()) return exTxn.get();
            }
            if (receiptNo != null) {
                Optional<InvoicePayment> exReceipt = payRepo.findByInvoiceIdAndReceiptNo(invoiceId, receiptNo);
                if (exReceipt.isPresent()) return exReceipt.get();
            }
            throw dup;
        }

        if (inv.getPaidAmount() == null) inv.setPaidAmount(BigDecimal.ZERO);
        inv.setPaidAmount(round(inv.getPaidAmount().add(p.getAmount())));

        // Ensure bill number/date exist whenever balance settles via payments
        if (inv.getBillNumber() == null) {
            inv.setBillNumber(billSeq.next(inv.getClinicId()));
        }
        if (inv.getIssuedAt() == null) {
            inv.setIssuedAt(LocalDateTime.now());
        }

        BigDecimal due = inv.getGrandTotal().subtract(inv.getPaidAmount());
        if (due.signum() <= 0) {
            inv.setStatus(Invoice.Status.PAID);
            inv.setBalanceDue(BigDecimal.ZERO);
        } else {
            inv.setStatus(Invoice.Status.PARTIALLY_PAID);
            inv.setBalanceDue(round(due));
        }

        invoiceRepo.save(inv);

        auditService.log(
                inv.getId(),
                inv.getAppointmentId(),
                InvoiceAuditLog.Action.PAYMENT_ADDED,
                req.receivedBy(),
                "amount=" + p.getAmount() + ", method=" + p.getMethod().name()
        );

        return p;
    }

    @Transactional
    public List<InvoicePayment> addPayments(Long invoiceId, List<InvoiceDtos.AddPaymentReq> payments) {
        List<InvoicePayment> out = new ArrayList<>();
        for (var p : payments) out.add(addPaymentRobust(invoiceId, p));
        return out;
    }

    // ---------- helpers ----------
    private void validateSamePayloadOrThrow(InvoicePayment existing,
                                            Long invoiceId,
                                            InvoicePayment.Method method,
                                            InvoiceDtos.AddPaymentReq req) {

        if (!Objects.equals(existing.getInvoice().getId(), invoiceId)) {
            throw new IdempotencyConflictException("Same key used for different invoiceId");
        }
        if (existing.getMethod() != method) {
            throw new IdempotencyConflictException("Same key reused with different method");
        }
        if (existing.getAmount() == null || req.amount() == null || existing.getAmount().compareTo(req.amount()) != 0) {
            throw new IdempotencyConflictException("Same key reused with different amount");
        }

        String reqTxn = normalizeNullable(req.txnRef());
        String exTxn = normalizeNullable(existing.getTxnRef());
        if (!Objects.equals(exTxn, reqTxn)) {
            throw new IdempotencyConflictException("Same key reused with different txnRef");
        }

        if (!Objects.equals(existing.getReceivedBy(), req.receivedBy())) {
            throw new IdempotencyConflictException("Same key reused with different receivedBy");
        }
        if (!Objects.equals(existing.getCardType(), req.cardType())) {
            throw new IdempotencyConflictException("Same key reused with different cardType");
        }
        if (!Objects.equals(existing.getPaymentDate(), req.paymentDate())) {
            throw new IdempotencyConflictException("Same key reused with different paymentDate");
        }
    }

    private String normalize(String s) {
        if (s == null) throw new IllegalArgumentException("required field missing");
        return s.trim();
    }

    private String normalizeNullable(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
