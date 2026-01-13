 package com.MediHubAPI.repository.billing;

import com.MediHubAPI.model.billing.ReceiptNumberSequence;
import com.MediHubAPI.model.billing.ReceiptNumberSequenceId;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ReceiptNumberSequenceRepository
        extends JpaRepository<ReceiptNumberSequence, ReceiptNumberSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select r from ReceiptNumberSequence r
        where r.clinicId = :clinicId and r.fy = :fy
    """)
    Optional<ReceiptNumberSequence> findForUpdate(String clinicId, int fy);
}
