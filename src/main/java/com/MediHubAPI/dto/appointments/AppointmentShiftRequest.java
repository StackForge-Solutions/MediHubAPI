package com.MediHubAPI.dto.appointments;

import com.MediHubAPI.model.enums.ShiftDirection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AppointmentShiftRequest {

    @NotNull(message = "doctorId is required")
    @Positive(message = "doctorId must be positive")
    private Long doctorId;

    @NotNull(message = "dateISO is required")
    private String dateISO;

    @NotNull(message = "startFromHHmm is required")
    private String startFromHHmm;

    @Positive(message = "minutes must be greater than 0")
    private int minutes;

    @NotNull(message = "direction is required")
    private ShiftDirection direction;

    private boolean sendSms = false;

    private String reason;
}
