package com.MediHubAPI.repository.projection;

import java.time.LocalDateTime;

public interface PharmacyQueueRowProjection {
    String getTokenNo();
    Long getPatientId();
    String getPatientName();
    String getDoctorName();
    LocalDateTime getCreatedAt();
    Integer getHasInsurance();
    Integer getHasReferrer();
    String getStatus();
}
