package com.MediHubAPI.service.api;

import com.MediHubAPI.dto.appointments.AppointmentBlockRequest;
import com.MediHubAPI.dto.appointments.AppointmentBlockResponse;
import com.MediHubAPI.dto.appointments.AppointmentShiftRequest;
import com.MediHubAPI.dto.appointments.AppointmentShiftResponse;
import com.MediHubAPI.dto.appointments.AppointmentUnblockRequest;
import com.MediHubAPI.dto.appointments.AppointmentUnblockResponse;

public interface AppointmentBlockApiService {

    AppointmentBlockResponse blockSlots(String idempotencyKey, AppointmentBlockRequest request);

    AppointmentUnblockResponse unblockSlots(String idempotencyKey, AppointmentUnblockRequest request);

    AppointmentShiftResponse shiftAppointments(String idempotencyKey, AppointmentShiftRequest request);
}
