package com.MediHubAPI.service.billing;

import com.MediHubAPI.dto.billing.InvoiceByAppointmentResponse;
import com.MediHubAPI.dto.billing.InvoiceFetchMode;

public interface InvoiceQueryService {

    InvoiceByAppointmentResponse getByAppointment(
            Long appointmentId,
            boolean includeItems,
            boolean includePayments,
            boolean includeAudit,
            InvoiceFetchMode mode
    );
}
