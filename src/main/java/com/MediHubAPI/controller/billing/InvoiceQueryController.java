package com.MediHubAPI.controller.billing;

import com.MediHubAPI.dto.ApiMeta;
import com.MediHubAPI.dto.DataEnvelope;
import com.MediHubAPI.dto.billing.InvoiceByAppointmentResponse;
import com.MediHubAPI.dto.billing.InvoiceDraftByAppointmentResponse;
import com.MediHubAPI.dto.billing.InvoiceFetchMode;
import com.MediHubAPI.service.billing.InvoiceQueryService;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceQueryController {

    private final InvoiceQueryService invoiceQueryService;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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

    @GetMapping("/v1/by-appointment/{appointmentId}")
    public DataEnvelope<InvoiceByAppointmentResponse> getExistingInvoiceByAppointment(
            @PathVariable Long appointmentId,
            @RequestParam(value = "includeItems", required = false, defaultValue = "true") boolean includeItems,
            @RequestParam(value = "includePayments", required = false, defaultValue = "true") boolean includePayments,
            @RequestParam(value = "includeAudit", required = false, defaultValue = "false") boolean includeAudit,
            @RequestParam(value = "mode", required = false, defaultValue = "ACTIVE") InvoiceFetchMode mode
    ) {
        log.info("API call: getExistingInvoiceByAppointment appointmentId={}, includeItems={}, includePayments={}, includeAudit={}, mode={}",
                appointmentId, includeItems, includePayments, includeAudit, mode);

        InvoiceByAppointmentResponse data = invoiceQueryService.getByAppointment(
                appointmentId, includeItems, includePayments, includeAudit, mode
        );

        return DataEnvelope.<InvoiceByAppointmentResponse>builder()
                .data(data)
                .meta(ApiMeta.builder()
                        .traceId(UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                        .timestamp(Instant.now())
                        .build())
                .build();
    }

@GetMapping("/all/by-appointment/{appointmentId}")
public DataEnvelope<InvoiceDraftByAppointmentResponse> getAllInvoicesByAppointmentId(
        @PathVariable Long appointmentId,
        @RequestParam(name = "queue", required = false) String queue
) {
    InvoiceDraftByAppointmentResponse data =
            invoiceQueryService.getAllInvoicesByAppointmentId(appointmentId, queue);

    return DataEnvelope.<InvoiceDraftByAppointmentResponse>builder()
            .data(data)
            .meta(ApiMeta.builder()
                    .traceId(UUID.randomUUID().toString().replace("-", "").substring(0, 16))
                    .timestamp(Instant.now())
                    .build())
            .build();
}


}
