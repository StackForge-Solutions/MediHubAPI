package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, Long> { }
