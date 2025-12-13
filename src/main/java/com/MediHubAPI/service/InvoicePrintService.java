package com.MediHubAPI.service;


import com.MediHubAPI.model.billing.*;
import com.MediHubAPI.repository.InvoicePaymentRepository;
import com.MediHubAPI.repository.InvoiceRepository;
import com.MediHubAPI.util.AgeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;


import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoicePrintService {

    private final InvoiceRepository invoiceRepo;
    private final InvoicePaymentRepository paymentRepo;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DTF =
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")
                    .withZone(ZoneId.systemDefault());

    /**
     * Build a printable view model for the invoice.
     * Loads items with a fetch-join and payments via a second query
     * to avoid MultipleBagFetchException.
     */
    @Transactional(readOnly = true)
    public PrintDtos.PrintInvoice build(Long id) {

        // Fetch invoice with doctor/patient + items (only one bag fetch)
        Invoice i = invoiceRepo.fetchForPrintWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));

        // Load payments separately (ordered latest-first)
        List<InvoicePayment> payments = paymentRepo.findByInvoiceIdOrderByReceivedAtDesc(id);

        // ----- Clinic Header (adjust to your config or pull from DB) -----
        PrintDtos.Clinic clinic = new PrintDtos.Clinic(
                "Nirmal Clinic",
                "#416/A, 2nd Floor, 11th B Cross, 1st Phase, JP Nagar, Bangalore – 560078",
                "9591027967",
                "lohith155@gmail.com",
                "nirmalclinic.in",
                "08046972434",
                null // logoUrl if you have one
        );

        // ----- Doctor & Patient display -----
        String doctorName = safeDoctorName(i);
        String patientName = safePatientName(i);
        String patientPhone = i.getPatient() != null ? nvl(i.getPatient().getMobileNumber()) : "";
        String patientIdDisplay = i.getPatient() != null && i.getPatient().getId() != null
                ? String.valueOf(i.getPatient().getId()) : "";
        String sex = (i.getPatient() != null && i.getPatient().getSex() != null)
                ? capitalize(i.getPatient().getSex().name()) : "";
        String age = (i.getPatient() != null && i.getPatient().getDateOfBirth() != null)
                ? AgeUtil.ageYMD(i.getPatient().getDateOfBirth(),
                i.getIssuedAt() != null ? i.getIssuedAt().toLocalDate() : java.time.LocalDate.now())
                : "";

        PrintDtos.Patient patient = new PrintDtos.Patient(
                patientName + (patientPhone.isBlank() ? "" : ("/" + patientPhone)),
                patientPhone,
                patientIdDisplay,
                (age.isBlank() ? "" : age) + (sex.isBlank() ? "" : "/" + sex)
        );

        PrintDtos.Doctor doctor = new PrintDtos.Doctor(doctorName);
        String billNo = nvl(i.getBillNumber());
        String dateTime = i.getIssuedAt() != null ? DTF.format(i.getIssuedAt()) : "";

        // ----- Line Items -----
        List<PrintDtos.LineItem> items = i.getItems().stream()
                .sorted(Comparator.comparing(InvoiceItem::getSlNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(it -> new PrintDtos.LineItem(
                        it.getSlNo() == null ? 0 : it.getSlNo(),
                        nvl(it.getName()),
                        zero(it.getUnitPrice()),
                        it.getQty() == null ? 1 : it.getQty(),
                        zero(it.getLineTotal())
                ))
                .toList();

        // ----- Totals -----
        BigDecimal totalBilled = zero(i.getGrandTotal());
        BigDecimal paid = zero(i.getPaidAmount());
        String amountInWords = AmountInWords.rupees(totalBilled.longValue()) + " Only";
        PrintDtos.Totals totals = new PrintDtos.Totals(totalBilled, paid, amountInWords);

        // ----- Receipt line (use latest payment if any) -----
        InvoicePayment latest = payments.isEmpty() ? null : payments.get(0);
        String receiptNo = billNo.isEmpty() ? "" : ("OP-" + billNo + "-1");
        String receiptLine = (latest == null) ? "" :
                String.format("%s  |  %s  |  Received Rs. %s (by %s) as Payment",
                        receiptNo,
                        DTF.format(latest.getReceivedAt()),
                        latest.getAmount().setScale(2),
                        latest.getMethod().name().replace('_', ' ')
                );

        String tokenQueueRoom = String.format("Token: %s, Queue: %s, Room: %s",
                nvl(i.getToken()), nvl(i.getQueue()), nvl(i.getRoom()));

        PrintDtos.Receipt receipt = new PrintDtos.Receipt(receiptNo, receiptLine, tokenQueueRoom);

        // ----- Final View Model -----
        return new PrintDtos.PrintInvoice(
                clinic,
                "OP Bill",
                billNo,
                dateTime,
                patient,
                doctor,
                items,
                totals,
                receipt,
                doctorName // signatory
        );
    }

    /** Render the Thymeleaf template to HTML string. */
    public String renderHtml(PrintDtos.PrintInvoice pi) {
        Context ctx = new Context();
        ctx.setVariable("pi", pi);
        return templateEngine.process("invoice-opd", ctx); // resources/templates/invoice-opd.html
    }

    /** Convert HTML to PDF bytes using OpenHTMLtoPDF. */
    public byte[] renderPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder =
                    new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render PDF", e);
        }
    }

    // ----------------- Helpers -----------------

    private static String nvl(Object o) {
        return (o == null) ? "" : o.toString();
    }

    private static BigDecimal zero(BigDecimal b) {
        return (b == null) ? BigDecimal.ZERO : b;
    }

    private static String capitalize(String s) {
        if (s == null || s.isBlank()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private static String safeDoctorName(Invoice i) {
        if (i.getDoctor() == null) return "";
        String fn = i.getDoctor().getFirstName() == null ? "" : i.getDoctor().getFirstName();
        String ln = i.getDoctor().getLastName() == null ? "" : i.getDoctor().getLastName();
        String name = (fn + " " + ln).trim();
        if (name.isBlank()) {
            name = i.getDoctor().getUsername() != null ? i.getDoctor().getUsername() : "";
        }
        // Prefix “Dr.” if not present
        String lower = name.toLowerCase();
        if (!lower.startsWith("dr ")) name = "Dr. " + name;
        return name.trim();
    }

    private static String safePatientName(Invoice i) {
        if (i.getPatient() == null) return "";
        String fn = i.getPatient().getFirstName() == null ? "" : i.getPatient().getFirstName();
        String ln = i.getPatient().getLastName() == null ? "" : i.getPatient().getLastName();
        String name = (fn + " " + ln).trim();
        if (name.isBlank()) {
            name = i.getPatient().getUsername() != null ? i.getPatient().getUsername() : "";
        }
        return name.trim();
    }
}
