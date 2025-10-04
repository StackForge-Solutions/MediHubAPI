// src/main/java/com/MediHubAPI/billing/service/BillNumberSequenceService.java
package com.MediHubAPI.service;

import org.springframework.stereotype.Service;

import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Replace with DB-backed sequence for multi-instance safety.
 */
@Service
public class BillNumberSequenceService {
    private final AtomicLong seq = new AtomicLong(12220); // seed from DB on startup

    public synchronized String next(String clinicId) {
        // Example: <FY>-<clinic>-<sequence>
        int fy = Year.now().getValue();
        long n = seq.incrementAndGet();
        return String.valueOf(n); // keep simple like sample "12220"
    }
}
