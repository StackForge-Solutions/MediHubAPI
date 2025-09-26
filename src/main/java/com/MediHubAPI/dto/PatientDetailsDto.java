package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.AppointmentType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class PatientDetailsDto {

    private Long patientId;
    private String patientName;
    private String hospitalId;
    private String phoneNumber;
    private AppointmentType visitType; // matches the enum type in Appointment
    private LocalDate appointmentDate;
    private LocalTime slotTime;
    private String doctorName;
    private String bookedBy;

    // Explicit constructor for JPA
    public PatientDetailsDto(Long patientId, String patientName, String hospitalId, String phoneNumber,
                             AppointmentType visitType, LocalDate appointmentDate, LocalTime slotTime,
                             String doctorName, String bookedBy) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.hospitalId = hospitalId;
        this.phoneNumber = phoneNumber;
        this.visitType = visitType;
        this.appointmentDate = appointmentDate;
        this.slotTime = slotTime;
        this.doctorName = doctorName;
        this.bookedBy = bookedBy;
    }
}
