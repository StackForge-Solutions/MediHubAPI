package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByBillNumber(String billNumber);
}
