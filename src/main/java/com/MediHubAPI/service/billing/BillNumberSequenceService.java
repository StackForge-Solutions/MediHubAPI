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
        String tenant = normalizeClinicId(clinicId);
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

    /**
     * Normalize clinic identifier so literal strings like "null"/"NULL"/"N/A"
     * don't leak into bill numbers. Falls back to "CLINIC" when blank.
     */
    private String normalizeClinicId(String clinicId) {
        if (clinicId == null) return "CLINIC";
        String trimmed = clinicId.trim();
        if (trimmed.isEmpty()) return "CLINIC";
        String upper = trimmed.toUpperCase();
        if (upper.equals("NULL") || upper.equals("N/A") || upper.equals("NONE")) {
            return "CLINIC";
        }
        return upper;
    }
}
