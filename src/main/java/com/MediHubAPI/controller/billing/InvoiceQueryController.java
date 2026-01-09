package com.MediHubAPI.controller.billing;

import com.MediHubAPI.dto.ApiMeta;
import com.MediHubAPI.dto.DataEnvelope;
import com.MediHubAPI.dto.billing.InvoiceByAppointmentResponse;
import com.MediHubAPI.dto.billing.InvoiceFetchMode;
import com.MediHubAPI.service.billing.InvoiceQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceQueryController {

    private final InvoiceQueryService invoiceQueryService;

    @GetMapping("/by-appointment/{appointmentId}")
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
}
