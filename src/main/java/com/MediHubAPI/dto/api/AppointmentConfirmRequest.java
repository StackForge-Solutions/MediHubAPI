package com.MediHubAPI.dto.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentConfirmRequest {

    @NotNull(message = "doctorId is required")
    private Long doctorId;

    @NotBlank(message = "dateISO is required")
    private String dateISO;

    @NotBlank(message = "timeHHmm is required")
    private String timeHHmm;

    @NotBlank(message = "slotStatus is required")
    private String slotStatus;

    @Valid
    @NotNull(message = "patient payload is required")
    private PatientBlock patient;

    @Valid
    @NotNull(message = "booking payload is required")
    private Booking booking;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientBlock {
        private Long id;
        private String fullName;
        private String phone;
        private String countryCode;
        private Boolean isInternational;
        @Valid
        private PatientDetails details;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientDetails {
        private String dobISO;
        private String govtIdType;
        private String govtIdNumber;
        @Valid
        private PatientAddress address;
        @Valid
        private PatientReferrer referrer;
        private String notes;
        private Boolean needsAttention;
        private String photoBase64;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientAddress {
        private String line1;
        private String area;
        private String city;
        private String pin;
        private String state;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientReferrer {
        private String type;
        private String name;
        private String phone;
        private String email;
        private String mainComplaint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Booking {
        private String visitCategory;
        private String source;
        private String visitType;
        private Integer slotsCount;
        private Boolean isWalkin;
        private Boolean markArrived;
        private Boolean needsAttention;
        private String notes;
    }
}
