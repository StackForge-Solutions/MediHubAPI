 package com.MediHubAPI.service.billing;

import com.MediHubAPI.model.billing.ReceiptNumberSequence;
import com.MediHubAPI.repository.billing.ReceiptNumberSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
@RequiredArgsConstructor
public class ReceiptNumberSequenceService {

    private final ReceiptNumberSequenceRepository repo;

    @Transactional
    public String nextReceiptNo(String clinicId, Long invoiceId) {
        int fy = Year.now().getValue();

        ReceiptNumberSequence row = repo.findForUpdate(clinicId, fy)
                .orElseGet(() -> {
                    ReceiptNumberSequence r = new ReceiptNumberSequence();
                    r.setClinicId(clinicId);
                    r.setFy(fy);
                    r.setNextVal(0L);
                    return repo.save(r);
                });

        long seq = row.getNextVal() + 1;
        row.setNextVal(seq);
        repo.save(row);

        // Example format
        return "RCPT-" + fy + "-" + clinicId + "-" + String.format("%06d", seq) + "-INV" + invoiceId;
    }
}
