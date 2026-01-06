package com.MediHubAPI.exception.emr;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;

public class PrescriptionTemplateNameExistsException extends HospitalAPIException {
    public PrescriptionTemplateNameExistsException(String name) {
        super(HttpStatus.CONFLICT, "TEMPLATE_NAME_EXISTS",
                "A prescription template named '" + name + "' already exists.");
    }
}
