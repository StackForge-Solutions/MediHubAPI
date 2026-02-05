package com.MediHubAPI.dto.appointments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AppointmentBlockRequest {

    @NotNull(message = "doctorId is required")
    @Positive(message = "doctorId must be positive")
    private Long doctorId;

    @NotNull(message = "dateISO is required")
    private String dateISO;

    @NotNull(message = "startHHmm is required")
    private String startHHmm;

    @NotNull(message = "endHHmm is required")
    private String endHHmm;

    private boolean cancelBooked = false;
}
