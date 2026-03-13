package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class OverReceiptException extends HospitalAPIException {
    public OverReceiptException(Long itemId, Integer requestedQty, Integer pendingQty) {
        super(HttpStatus.CONFLICT, "OVER_RECEIPT",
                "Received quantity " + requestedQty + " exceeds pending quantity " + pendingQty + " for purchase order item " + itemId);
    }
}
