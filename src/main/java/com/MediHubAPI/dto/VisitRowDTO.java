package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.AppointmentType;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalTime;

@Value
public class VisitRowDTO {
    Long appointmentId;

    Long doctorId;
    String doctorName;

    Long patientId;
    String patientName;
    String patientPhone;

    LocalDate appointmentDate;
    LocalTime slotTime;

    AppointmentType visitType;
    String bookedBy; // who created/ booked

    // Optional extras you may want to return:
    LocalTime slotStartTime;
    LocalTime slotEndTime;

    Long invoiceId;
    boolean paid; // true if invoice is fully paid
}
