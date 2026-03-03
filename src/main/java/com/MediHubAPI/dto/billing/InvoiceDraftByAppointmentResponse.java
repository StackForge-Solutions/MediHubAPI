package com.MediHubAPI.dto.billing;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDraftByAppointmentResponse {

    //  NEW: if invoice exists
    private Long invoiceId;     // null if no invoice yet
    private String status;      // e.g. "DRAFT", "ISSUED", "PAID", "VOID", ...

    private Long appointmentId;
    private String tokenNo;
    private Boolean draft;

    private SourceRef source;

    private String patientName;
    private String doctorName;
    private String department;

    private List<DraftItem> items;

    private Double subTotal;
    private Double discountTotal;
    private Double taxTotal;
    private Double netPayable;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SourceRef {
        private Long prescriptionId;
        private Long visitSummaryId;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class DraftItem {
        private Long id;                 // sequential row in response OR prescribedTestId
        private String type;             // "LAB"
        private String serviceCode;      // "LAB_HBA1C"
        private String serviceName;      // "HbA1c"
        private Double unitPrice;
        private Integer quantity;
        private Double discount;
        private Boolean taxable;
        private Double lineTotal;

        private SourceRefItem sourceRef;

        @Data @NoArgsConstructor @AllArgsConstructor @Builder
        public static class SourceRefItem {
            private String kind;         // "PRESCRIBED_TEST"
            private String refId;        // "ptest-991"
        }
    }
}
