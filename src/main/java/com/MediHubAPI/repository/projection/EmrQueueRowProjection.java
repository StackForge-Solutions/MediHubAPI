package com.MediHubAPI.repository.projection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public interface EmrQueueRowProjection {
    String getTokenNo();

    Long getDoctorId();

    Long getAppointmentId();

    LocalTime getSlotTime();

    LocalDate getVisitDate();

    Long getPatientId();

    String getPatientName();

    String getSex();

    LocalDate getDob();

    String getDoctorName();

    LocalDateTime getCreatedAt();

    String getAppointmentStatus();

    String getInvoiceQueue();

    String getInvoiceNotes();

    String getReferrerName();

    String getReferrerNumber();

    String getReferrerType();
}