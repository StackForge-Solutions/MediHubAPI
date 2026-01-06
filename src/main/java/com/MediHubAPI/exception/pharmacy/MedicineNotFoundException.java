package com.MediHubAPI.exception.pharmacy;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class MedicineNotFoundException extends HospitalAPIException {
    public MedicineNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "MEDICINE_NOT_FOUND", "Medicine not found: " + id);
    }
}
