package com.MediHubAPI.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.MediHubAPI.dto.InvoiceDtos;
import com.MediHubAPI.model.billing.Invoice;
import com.MediHubAPI.repository.projection.PharmacyQueueRowProjection;

public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

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

    @Query(value = """
            select concat('TKN-', lpad(i.token_no, 3, '0')) as tokenNo,
                   pu.hospital_id                           as patientId,
                   concat(pu.first_name, ' ', pu.last_name) as patientName,
                   concat(du.first_name, ' ', du.last_name) as doctorName,
                   i.created_at                             as createdAt,
                   if(upper(coalesce(i.queue, '')) like '%INSUR%', 1, 0)                                  as hasInsurance,
                   if(pat.referrer_name is not null or pat.referrer_number is not null, 1, 0)             as hasReferrer,
                   case i.status
                       when 'DRAFT'  then 'Waiting'
                       when 'ISSUED' then 'Waiting'
                       when 'PAID'   then 'Completed'
                       else i.status
                   end                                            as status
            from invoices i
                     join users pu on pu.id = i.patient_id
                     join users du on du.id = i.doctor_id
                     left join patients pat on pat.user_id = i.patient_id
            where i.created_at >= :start
              and i.created_at < :end
            order by i.created_at asc
            """,
            nativeQuery = true)
//    and (i.queue is null or upper(i.queue) like '%PHARM%')

    List<PharmacyQueueRowProjection> fetchPharmacyQueue(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}
