package com.MediHubAPI.repository.projection;

import java.time.LocalDateTime;

public interface LabQueueRowProjection {
    String getToken();
    Long getPatientId();
    String getPatientName();
    String getAgeLabel();
    String getPhone();
    String getDoctorName();
    String getReferrerName();
    LocalDateTime getCreatedAt();
    String getDateISO();
    String getStatus();
    Integer getInsurance();
    Integer getReferrer();
    String getNotes();
    String getRoom();
}
