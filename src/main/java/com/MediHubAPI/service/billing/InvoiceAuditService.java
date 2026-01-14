package com.MediHubAPI.service.billing;

import com.MediHubAPI.model.billing.InvoiceAuditLog;
import com.MediHubAPI.repository.InvoiceAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceAuditService {

    private final InvoiceAuditLogRepository repo;

    @Transactional
    public void log(Long invoiceId,
                    Long appointmentId,
                    InvoiceAuditLog.Action action,
                    String actor,
                    String reason) {

        InvoiceAuditLog row = InvoiceAuditLog.builder()
                .invoiceId(invoiceId)
                .appointmentId(appointmentId)
                .action(action)
                .actor(actor)
                .reason(reason)
                .build();

        repo.save(row);
    }
}
