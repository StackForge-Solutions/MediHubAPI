package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PurchaseOrderReceiptNotAllowedException extends HospitalAPIException {
    public PurchaseOrderReceiptNotAllowedException(Long id, String status) {
        super(HttpStatus.CONFLICT, "PURCHASE_ORDER_RECEIPT_NOT_ALLOWED",
                "Purchase order " + id + " cannot be received in status " + status);
    }
}
