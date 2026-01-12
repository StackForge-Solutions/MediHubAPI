package com.MediHubAPI.repository;

import com.MediHubAPI.model.billing.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
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


    Optional<Invoice> findFirstByAppointmentIdAndStatusIn(Long appointmentId,
                                                          Collection<Invoice.Status> statuses);


    @EntityGraph(attributePaths = {"items"})
    Optional<Invoice> findTopByAppointmentIdOrderByCreatedAtDesc(
            Long appointmentId
     );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);


}
