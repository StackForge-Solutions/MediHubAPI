package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PharmacyTransactionNotFoundException extends HospitalAPIException {
    public PharmacyTransactionNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "PHARMACY_TRANSACTION_NOT_FOUND", "Pharmacy transaction not found: " + id);
    }
}
