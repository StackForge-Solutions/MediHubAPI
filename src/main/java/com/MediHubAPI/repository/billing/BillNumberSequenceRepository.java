package com.MediHubAPI.repository.billing;


import com.MediHubAPI.model.billing.BillNumberSequence;
import com.MediHubAPI.model.billing.BillNumberSequenceId;
import org.springframework.data.jpa.repository.*;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface BillNumberSequenceRepository
        extends JpaRepository<BillNumberSequence, BillNumberSequenceId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from BillNumberSequence b
        where b.clinicId = :clinicId and b.fy = :fy
    """)
    Optional<BillNumberSequence> findForUpdate(String clinicId, int fy);
}
