package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.InvoicePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    // Paged + sortable
    Page<InvoicePayment> findByInvoiceId(Long invoiceId, Pageable pageable);

    // Non-paged convenience
    List<InvoicePayment> findByInvoiceIdOrderByReceivedAtDesc(Long invoiceId);
}
