package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class DuplicateVendorCodeException extends HospitalAPIException {
    public DuplicateVendorCodeException(String vendorCode) {
        super(HttpStatus.CONFLICT, "DUPLICATE_VENDOR_CODE", "Vendor code already exists: " + vendorCode);
    }
}
