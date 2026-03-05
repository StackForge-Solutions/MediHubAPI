package com.MediHubAPI.exception.diagnosis;

import org.springframework.http.HttpStatus;

public class DiagnosisAppointmentNotFoundException extends DiagnosisException {

    public DiagnosisAppointmentNotFoundException(Long appointmentId) {
        super(HttpStatus.NOT_FOUND, "APPOINTMENT_NOT_FOUND",
                "Appointment not found for appointmentId=" + appointmentId);
    }
}
