 package com.MediHubAPI.service.billing;

import com.MediHubAPI.model.billing.BillNumberSequence;
 import com.MediHubAPI.repository.billing.BillNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import java.time.Year;

 @Service
 @RequiredArgsConstructor
 public class BillNumberSequenceService {

     private final BillNumberSequenceRepository repo;

    @Transactional
    public String next(String clinicId) {
        // Normalize clinicId to avoid "null" in bill numbers
        String tenant = (clinicId == null || clinicId.isBlank()) ? "CLINIC" : clinicId.trim().toUpperCase();
        int fy = Year.now().getValue(); // or your FY logic

        BillNumberSequence row = repo.findForUpdate(tenant, fy)
                .orElseGet(() -> {
                    BillNumberSequence r = new BillNumberSequence();
                    r.setClinicId(tenant);
                    r.setFy(fy);
                    r.setNextVal(0L);     // first call becomes 1
                    return repo.save(r);
                });

        long seq = row.getNextVal() + 1;
        row.setNextVal(seq);
        repo.save(row);

        //  Option 1 format: FY-CLINIC-SEQ
        return fy + "-" + tenant + "-" + String.format("%06d", seq);
    }
}
