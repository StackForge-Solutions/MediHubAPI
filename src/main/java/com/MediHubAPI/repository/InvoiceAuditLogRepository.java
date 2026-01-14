package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.InvoiceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceAuditLogRepository extends JpaRepository<InvoiceAuditLog, Long> { }
