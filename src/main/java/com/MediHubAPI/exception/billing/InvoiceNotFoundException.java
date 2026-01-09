package com.MediHubAPI.exception.billing;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class InvoiceNotFoundException extends HospitalAPIException {
    public InvoiceNotFoundException(Long appointmentId) {
        super(HttpStatus.NOT_FOUND,
                "INVOICE_NOT_FOUND",
                "No invoice exists for appointmentId=" + appointmentId);
    }
}
