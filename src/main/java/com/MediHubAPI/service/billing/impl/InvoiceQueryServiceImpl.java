package com.MediHubAPI.service.billing.impl;

import com.MediHubAPI.dto.billing.*;
import com.MediHubAPI.exception.billing.InvoiceNotFoundException;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.model.billing.InvoiceItem;
import com.MediHubAPI.model.billing.InvoicePayment;
import com.MediHubAPI.repository.InvoiceItemRepository;
import com.MediHubAPI.repository.InvoicePaymentRepository;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.service.billing.InvoiceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceQueryServiceImpl implements InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;


    private final com.MediHubAPI.repository.VisitSummaryRepository visitSummaryRepository;
    private final com.MediHubAPI.repository.PrescribedTestRepository prescribedTestRepository;
    private final com.MediHubAPI.repository.emr.PrescriptionRepository prescriptionRepository;
    private final com.MediHubAPI.repository.AppointmentRepository appointmentRepository;

    @Override
    @Transactional(readOnly = true)
    public InvoiceByAppointmentResponse getByAppointment(Long appointmentId,
                                                         boolean includeItems,
                                                         boolean includePayments,
                                                         boolean includeAudit,
                                                         InvoiceFetchMode mode) {

        Invoice invoice = (mode == InvoiceFetchMode.LATEST)
                ? findLatestInvoice(appointmentId)
                : findActiveInvoice(appointmentId);

        if (invoice == null) {
            throw new InvoiceNotFoundException(appointmentId);
        }

        // -------- items ----------
        List<InvoiceItemDto> items = Collections.emptyList();
        if (includeItems) {
            // If your Invoice entity already has i.items and it's safe to use, you can use invoice.getItems()
            // But to keep it stable (and avoid multiple bag issues), query item table separately if you have relation fields.
            List<InvoiceItem> entityItems = invoice.getItems() == null ? List.of() : invoice.getItems();

            items = entityItems.stream()
                    .map(this::toItemDto)
                    .collect(Collectors.toList());
        }

        // -------- payments ----------
        List<PaymentMethodDto> methods = Collections.emptyList();
        String payStatus = invoice.getStatus() == null ? null : invoice.getStatus().name();

        if (includePayments) {
            List<InvoicePayment> pays = invoicePaymentRepository.findByInvoiceIdOrderByReceivedAtDesc(invoice.getId());
            methods = pays.stream().map(p -> PaymentMethodDto.builder()
                            .mode(p.getMethod() == null ? null : p.getMethod().name())  // adjust if your method is String
                            .amount(p.getAmount().doubleValue())
                            .refNo(p.getTxnRef())
                            .build())
                    .collect(Collectors.toList());
        }

        InvoiceSummaryDto summary = InvoiceSummaryDto.builder()
                .subTotal(invoice.getSubTotal().doubleValue())
                .discountTotal(invoice.getDiscountTotal().doubleValue())
                .taxTotal(invoice.getTaxTotal().doubleValue())
                .netPayable(invoice.getGrandTotal().doubleValue())
                .amountPaid(invoice.getPaidAmount().doubleValue())
                .balance(invoice.getBalanceDue().doubleValue())
                .build();

        return InvoiceByAppointmentResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceNo(invoice.getBillNumber())         // your entity column is bill_number
                .status(invoice.getStatus() == null ? null : invoice.getStatus().name())
                .appointmentId(invoice.getAppointmentId())
                .tokenNo("")
                .invoiceDateTimeISO(invoice.getIssuedAt() == null ? null : invoice.getIssuedAt().toInstant(ZoneOffset.UTC).toString())

                // patient block (based on your entity associations)
                .patient(InvoiceByAppointmentResponse.PatientBlock.builder()
                        .uhid(null) // if you have UHID field, map it here
                        .name(invoice.getPatient() == null ? null : fullName(invoice.getPatient().getFirstName(), invoice.getPatient().getLastName()))
                        .phone(invoice.getPatient() == null ? null : invoice.getPatient().getMobileNumber())
                        .ageSexLabel(null) // compute if DOB/sex available
                        .build())

                // doctor block
                .doctor(InvoiceByAppointmentResponse.DoctorBlock.builder()
                        .id(invoice.getDoctor() == null ? null : invoice.getDoctor().getId())
                        .name(invoice.getDoctor() == null ? null : fullName(invoice.getDoctor().getFirstName(), invoice.getDoctor().getLastName()))
                        .department(null) // if you have specialization/department, map it
                        .build())

                .items(items)
                .summary(summary)
                .payment(InvoiceByAppointmentResponse.PaymentBlock.builder()
                        .status(payStatus)
                        .methods(methods)
                        .build())

                .preparedBy(invoice.getCreatedBy())
                .reprintCount(null) // if you have it in DB, map it
                .createdAtISO(invoice.getCreatedAt() == null ? null : invoice.getCreatedAt().toInstant(ZoneOffset.UTC).toString())
                .updatedAtISO(null) // if you have updatedAt
                .build();
    }

    private Invoice findActiveInvoice(Long appointmentId) {
        // ACTIVE = not cancelled (adapt statuses to your enum)
        // Example: PAID, UNPAID, PARTIAL. Exclude CANCELLED/VOID.
        List<Invoice.Status> active = List.of(
                Invoice.Status.PAID
                // add: Invoice.Status.UNPAID, Invoice.Status.PARTIAL etc as per your enum
        );
        return invoiceRepository.findFirstByAppointmentIdAndStatusIn(appointmentId, active).orElse(null);
    }

    private Invoice findLatestInvoice(Long appointmentId) {
        return invoiceRepository.findTopByAppointmentIdOrderByCreatedAtDesc(appointmentId).orElse(null);
    }


    private InvoiceItemDto toItemDto(InvoiceItem it) {
        return InvoiceItemDto.builder()
                .id(it.getId())
                .type(null) // or "LAB"
                .serviceCode(it.getServiceItemId() == null ? null : String.valueOf(it.getServiceItemId()))
                .serviceName(it.getName())
                .unitPrice(toDouble(it.getUnitPrice()))
                .quantity(it.getQty())
                .discount(toDouble(it.getDiscountAmount()))
                .taxable(isPositive(it.getTaxPercent()))
                .lineTotal(toDouble(it.getLineTotal()))
                .build();
    }

    private Double toDouble(BigDecimal v) {
        return v == null ? 0.0 : v.doubleValue();
    }

    private boolean isPositive(BigDecimal v) {
        return v != null && v.compareTo(BigDecimal.ZERO) > 0;
    }

    private double n(Double v) { return v == null ? 0.0 : v; }

    private String fullName(String f, String l) {
        String ff = f == null ? "" : f.trim();
        String ll = l == null ? "" : l.trim();
        String out = (ff + " " + ll).trim();
        return out.isEmpty() ? null : out;
    }
    @Transactional(readOnly = true)
    @Override
    public InvoiceDraftByAppointmentResponse getDraftByAppointment(Long appointmentId) {

        var appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid appointmentId=" + appointmentId));

        var vs = visitSummaryRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("VisitSummary not found for appointmentId=" + appointmentId));

        var prescriptionOpt = prescriptionRepository.findByAppointment_Id(appointmentId);

        var tests = prescribedTestRepository.findByVisitSummary_Id(vs.getId());

        List<InvoiceDraftByAppointmentResponse.DraftItem> items = new ArrayList<>();
        int sl = 1;

        for (var t : tests) {
            var master = t.getPathologyTestMaster();

            String code = null;
            if (master != null && master.getCode() != null && !master.getCode().isBlank()) {
                code = master.getCode();                  // e.g. "CBC"
            } else if (master != null && master.getId() != null) {
                code = "LAB_" + master.getId();           // fallback
            } else {
                code = "LAB_PTEST_" + t.getId();          // fallback
            }

            String serviceCode = code.startsWith("LAB_") ? code : "LAB_" + code; // "LAB_CBC" if code="CBC"

            double unitPrice = (t.getPrice() == null ? 0.0 : t.getPrice());
            int qty = (t.getQuantity() == null ? 1 : t.getQuantity());
            double discount = 0.0;
            boolean taxable = true;

            double lineTotal = (unitPrice * qty) - discount;

            items.add(InvoiceDraftByAppointmentResponse.DraftItem.builder()
                    .id((long) sl++)
                    .type("LAB")
                    .serviceCode(serviceCode)
                    .serviceName(t.getName())
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .discount(discount)
                    .taxable(taxable)
                    .lineTotal(lineTotal)
                    .sourceRef(InvoiceDraftByAppointmentResponse.DraftItem.SourceRefItem.builder()
                            .kind("PRESCRIBED_TEST")
                            .refId("ptest-" + t.getId())
                            .build())
                    .build());
        }

        double subTotal = items.stream().mapToDouble(InvoiceDraftByAppointmentResponse.DraftItem::getLineTotal).sum();
        double discountTotal = 0.0;
        double taxTotal = 0.0;
        double netPayable = subTotal; // if tax/discount later, adjust here

        String patientName = appt.getPatient() == null ? null : fullName(appt.getPatient().getFirstName(), appt.getPatient().getLastName());
        String doctorName = appt.getDoctor() == null ? null : fullName(appt.getDoctor().getFirstName(), appt.getDoctor().getLastName());

        return InvoiceDraftByAppointmentResponse.builder()
                .appointmentId(appointmentId)
                .tokenNo("TKN-" + String.format("%03d", appointmentId)) // or use invoice token logic if you have it
                .draft(true)
                .source(InvoiceDraftByAppointmentResponse.SourceRef.builder()
                        .prescriptionId(prescriptionOpt.map(p -> p.getId()).orElse(null))
                        .visitSummaryId(vs.getId())
                        .build())
                .patientName(patientName)
                .doctorName(doctorName)
                .department(null) // if doctor specialization exists, map it
                .items(items)
                .subTotal(subTotal)
                .discountTotal(discountTotal)
                .taxTotal(taxTotal)
                .netPayable(netPayable)
                .build();
    }

}
