package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PurchaseOrderStateException extends HospitalAPIException {
    public PurchaseOrderStateException(String code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }
}
