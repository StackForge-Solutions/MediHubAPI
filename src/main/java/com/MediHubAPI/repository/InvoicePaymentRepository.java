package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.InvoicePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> {

    // Paged + sortable
    Page<InvoicePayment> findByInvoiceId(Long invoiceId, Pageable pageable);

    // Non-paged convenience
    List<InvoicePayment> findByInvoiceIdOrderByReceivedAtDesc(Long invoiceId);

    /**
     * ✅ Checks if a transaction reference already exists for the same invoice.
     */
    @Query("""
           SELECT COUNT(p) > 0 
           FROM InvoicePayment p 
           WHERE p.txnRef = :txnRef 
             AND p.invoice.id = :invoiceId
           """)
    boolean existsByTxnRefAndInvoiceId(String txnRef, Long invoiceId);


    Optional<InvoicePayment> findByInvoiceIdAndIdempotencyKey(Long invoiceId, String idempotencyKey);

    Optional<InvoicePayment> findByMethodAndTxnRef(InvoicePayment.Method method, String txnRef);

    Optional<InvoicePayment> findByInvoiceIdAndReceiptNo(Long invoiceId, String receiptNo);

}
