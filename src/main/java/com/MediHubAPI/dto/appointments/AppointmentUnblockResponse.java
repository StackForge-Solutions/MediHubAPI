package com.MediHubAPI.dto.appointments;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AppointmentUnblockResponse {

    private List<String> unblockedSlots;
}
