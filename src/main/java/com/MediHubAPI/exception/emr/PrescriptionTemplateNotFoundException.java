package com.MediHubAPI.exception.emr;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PrescriptionTemplateNotFoundException extends HospitalAPIException {
    public PrescriptionTemplateNotFoundException(String tplId) {
        super(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "Prescription template not found: " + tplId);
    }
}
