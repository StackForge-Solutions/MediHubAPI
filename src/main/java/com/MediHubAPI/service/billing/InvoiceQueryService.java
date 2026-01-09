package com.MediHubAPI.service.billing;

import com.MediHubAPI.dto.billing.InvoiceByAppointmentResponse;
import com.MediHubAPI.dto.billing.InvoiceDraftByAppointmentResponse;
import com.MediHubAPI.dto.billing.InvoiceFetchMode;
import org.springframework.transaction.annotation.Transactional;

public interface InvoiceQueryService {

    InvoiceByAppointmentResponse getByAppointment(
            Long appointmentId,
            boolean includeItems,
            boolean includePayments,
            boolean includeAudit,
            InvoiceFetchMode mode
    );


    @Transactional(readOnly = true)
    InvoiceDraftByAppointmentResponse getDraftByAppointment(Long appointmentId);
}
