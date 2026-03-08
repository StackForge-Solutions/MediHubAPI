package com.MediHubAPI.repository.projection;

import java.time.LocalDateTime;

public interface PharmacyQueueRowProjection {
    String getTokenNo();
    String getPatientId();
    String getPatientName();
    String getDoctorName();
    LocalDateTime getCreatedAt();
    Boolean getHasInsurance();
    Boolean getHasReferrer();
    String getStatus();
}
