package com.MediHubAPI.service;

import com.MediHubAPI.dto.*;
import com.MediHubAPI.model.Appointment;
import com.MediHubAPI.model.Slot;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface SlotService {
    ShiftAppointmentsResult shiftSlots(Long doctorId, SlotShiftRequestDto request) ;
    void blockSlots(Long doctorId, SlotBlockRequestDto request);
    void unblockSlots(Long doctorId, SlotUnblockRequestDto request);
    List<SlotStatusDto> getSlotStatuses(Long doctorId, LocalDate date);
    Appointment bookWalkInSlot(WalkInAppointmentDto dto);
    List<Slot> getEmergencySlots(Long doctorId, LocalDate date);

    Slot getSlotByDoctorAndTime(Long id, @NotNull LocalDate appointmentDate, @NotNull LocalTime slotTime);
}
