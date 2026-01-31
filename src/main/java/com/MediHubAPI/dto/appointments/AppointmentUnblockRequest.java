package com.MediHubAPI.dto.appointments;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AppointmentUnblockRequest {

    @NotNull(message = "doctorId is required")
    @Positive(message = "doctorId must be positive")
    private Long doctorId;

    @NotNull(message = "dateISO is required")
    private String dateISO;
}
