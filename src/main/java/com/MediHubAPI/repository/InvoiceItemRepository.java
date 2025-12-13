// src/main/java/com/MediHubAPI/billing/repo/InvoiceItemRepository.java
package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> { }
