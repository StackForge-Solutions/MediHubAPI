package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PharmacyVendorNotFoundException extends HospitalAPIException {
    public PharmacyVendorNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "PHARMACY_VENDOR_NOT_FOUND", "Pharmacy vendor not found: " + id);
    }
}
