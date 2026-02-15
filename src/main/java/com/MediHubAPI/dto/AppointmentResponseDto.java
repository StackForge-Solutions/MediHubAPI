package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.AppointmentType;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AppointmentResponseDto {
    private Long id;
    private String doctorName;
    private String patientName;
    private LocalDate appointmentDate;
    private LocalTime slotTime;
    private String status;
    private AppointmentType type;
    private SlotInfoDto slot; //  New slot info
}
