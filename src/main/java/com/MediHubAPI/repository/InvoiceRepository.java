package com.MediHubAPI.repository;

import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.model.billing.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    @EntityGraph(attributePaths = {"items"})
    Optional<Invoice> findTopByAppointmentIdAndItems_ItemTypeOrderByCreatedAtDesc(
            Long appointmentId,
            InvoiceDtos.ItemType itemType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"items"})
    @Query("""
                select i
                from Invoice i
                where i.appointmentId = :appointmentId
                  and (:queue is null or i.queue = :queue)
                order by i.createdAt desc
            """)
    Optional<Invoice> findLatestByAppointmentIdAndOptionalQueue(
            @Param("appointmentId") Long appointmentId,
            @Param("queue") String queue
    );

// ======================= Repository =======================
// InvoiceRepository.java
// NOTE: This is JPQL (entity query) so PESSIMISTIC_WRITE lock is valid.

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
   select i from Invoice i
   where i.appointmentId = :appointmentId
     and (:queueLike is null or lower(i.queue) like lower(:queueLike))
   order by i.createdAt desc
""")
    List<Invoice> findLatestByAppointmentIdForUpdate(
            @Param("appointmentId") Long appointmentId,
            @Param("queueLike") String queueLike,
            Pageable pageable
    );

}
