package com.MediHubAPI.model.billing;



import java.math.BigDecimal;
import java.util.List;

public class PrintDtos {
    public record Clinic(String name, String address, String phone, String email,
                         String website, String ivrs, String logoUrl) {}

    public record Patient(String name, String contact, String idDisplay, String ageSex) {}

    public record Doctor(String name) {}

    public record LineItem(int slNo, String particulars, BigDecimal charges,
                           int qty, BigDecimal amount) {}

    public record Totals(BigDecimal totalBilled, BigDecimal paid, String amountInWords) {}

    public record Receipt(String receiptNo, String line, String tokenQueueRoom) {}

    public record PrintInvoice(Clinic clinic, String title, String billNo, String dateTime,
                               Patient patient, Doctor doctor,
                               List<LineItem> items, Totals totals,
                               Receipt receipt, String signatory) {}
}

