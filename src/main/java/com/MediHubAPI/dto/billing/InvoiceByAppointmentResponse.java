package com.MediHubAPI.dto.billing;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceByAppointmentResponse {

    private Long invoiceId;
    private String invoiceNo;
    private String status;
    private Long appointmentId;
    private String tokenNo;
    private String invoiceDateTimeISO;

    private PatientBlock patient;
    private DoctorBlock doctor;

    private List<InvoiceItemDto> items;

    private InvoiceSummaryDto summary;
    private PaymentBlock payment;

    private String preparedBy;
    private Integer reprintCount;

    private String createdAtISO;
    private String updatedAtISO;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PatientBlock {
        private String uhid;
        private String name;
        private String phone;
        private String ageSexLabel;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DoctorBlock {
        private Long id;
        private String name;
        private String department;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PaymentBlock {
        private String status; // PAID / PARTIAL / UNPAID
        private List<PaymentMethodDto> methods;
    }
}
