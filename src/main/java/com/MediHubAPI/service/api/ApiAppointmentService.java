package com.MediHubAPI.service.api;

import com.MediHubAPI.dto.api.AppointmentConfirmRequest;
import com.MediHubAPI.dto.api.AppointmentConfirmResponse;

public interface ApiAppointmentService {
    AppointmentConfirmResponse confirmBooking(AppointmentConfirmRequest request);
}
