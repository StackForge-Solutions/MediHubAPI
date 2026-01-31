package com.MediHubAPI.dto.appointments;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppointmentShiftResponse {

    private int shiftedCount;
    private int attentionCount;
}
