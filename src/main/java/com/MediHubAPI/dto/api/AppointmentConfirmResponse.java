package com.MediHubAPI.dto.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentConfirmResponse {
    private String appointmentId;
    private String tokenNo;
    private String status;
    private String smsStatus;
    private String createdAtISO;
    private PatientResponse patient;
    private DoctorResponse doctor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientResponse {
        private Long id;
        private String hospitalId;
        private String registrationId;
        private String newPatientHospitalId;
        private String fullName;
        private String phone;
        private Boolean needsAttention;
        private Boolean isInternational;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorResponse {
        private Long id;
        private String name;
        private String speciality;
        private Long departmentId;
    }
}
