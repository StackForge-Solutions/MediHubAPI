package com.MediHubAPI.dto.patient.search;

import com.MediHubAPI.exception.HospitalAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.Arrays;

public enum PatientSearchBy {
    NAME,
    PHONE,
    HOSPITAL_ID,
    FILE_NO,
    FATHER_NAME,
    MOTHER_NAME,
    DOB;

    public static PatientSearchBy from(String value) {
        if (!StringUtils.hasText(value)) {
            throw new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "by is required");
        }
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new HospitalAPIException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "by is invalid"));
    }
}
