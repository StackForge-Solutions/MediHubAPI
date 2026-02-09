package com.MediHubAPI.dto.appointments;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AppointmentBlockResponse {

    private List<String> blockedSlots;
    private int cancelledAppointments;
}
