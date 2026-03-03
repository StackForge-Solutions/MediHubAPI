package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.dto.InvoiceDtos;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientLabInvoiceRepository extends JpaRepository<Invoice, Long> {

    @EntityGraph(attributePaths = {"items", "patient", "patient.specialization"})
    Optional<Invoice> findTopByPatient_IdOrderByCreatedAtDesc(Long patientId);

    @EntityGraph(attributePaths = {"items", "patient", "patient.specialization"})
    Optional<Invoice> findTopByPatient_IdAndItems_ItemTypeOrderByCreatedAtDesc(
            Long patientId,
            InvoiceDtos.ItemType itemType
    );
}
