package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PurchaseOrderNotFoundException extends HospitalAPIException {
    public PurchaseOrderNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "PURCHASE_ORDER_NOT_FOUND", "Purchase order not found: " + id);
    }
}
