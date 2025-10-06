package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

 import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByBillNumber(String billNumber);

    // Avoid MultipleBagFetchException: fetch ONE bag (items) and single-valued associations.
    @Query(
            "select distinct i from Invoice i " +
                    "left join fetch i.items it " +
                    "left join fetch i.doctor d " +
                    "left join fetch i.patient p " +
                    "where i.id = :id"
    )
    Optional<Invoice> fetchForPrintWithItems(@Param("id") Long id);

    Optional<Invoice> findFirstByAppointmentIdAndStatus(Long appointmentId, Invoice.Status status);
}
