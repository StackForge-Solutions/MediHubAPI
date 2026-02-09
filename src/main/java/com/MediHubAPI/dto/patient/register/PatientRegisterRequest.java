package com.MediHubAPI.dto.patient.register;

import com.MediHubAPI.model.enums.BloodGroup;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Data
public class PatientRegisterRequest {

    @NotBlank
    @Pattern(regexp = "\\d{10,13}", message = "phone must be 10-13 digits")
    private String phone;

    private String fullName;
    private Boolean isInternational;
    private String photoBase64;

    @Size(max = 8)
    private String countryCode;

    @Pattern(regexp = "(?:\\d{10,13})?", message = "landline must be 10-13 digits")
    private String landline;

    @Valid
    @NotNull
    private Demographics demographics;

    @Valid
    private Address address;

    @Valid
    private Referrer referrer;

    @Valid
    private Notes notes;

    private BloodGroup bloodGroup;
    private String alternateContact;
    private String fatherName;
    private String motherName;
    private String spouseName;
    private String education;
    private String occupation;
    private String religion;
    private Double birthWeight;

    @AssertTrue(message = "referrerPhone must be 10-13 digits")
    public boolean isReferrerPhoneValid() {
        String phone = referrer == null ? null : referrer.getPhone();
        return phone == null || phone.isBlank() || phone.matches("\\d{10,13}");
    }

    @AssertTrue(message = "dobISO must not be in the future")
    public boolean isDobNotFuture() {
        if (demographics == null) return true;
        String iso = demographics.getDobISO();
        if (iso == null || iso.isBlank()) return true;
        try {
            LocalDate parsed = LocalDate.parse(iso);
            return !parsed.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    @Data
    public static class Demographics {
        @Size(max = 20)
        private String title;

        @NotBlank
        @Size(min = 1, max = 80)
        private String firstName;

        @Size(max = 80)
        private String lastName;

        private String fileNo;
        private String dobISO;

        @Min(0)
        @Max(150)
        private Integer age;

        private String gender;
        private String maritalStatus;
        private String motherTongue;
        private String email;

        private String govtIdType;
        private String govtIdNo;

        @AssertTrue(message = "govtIdType and govtIdNo must both be provided")
        public boolean isGovtIdPairValid() {
            return (govtIdType == null && govtIdNo == null) || (govtIdType != null && govtIdNo != null);
        }
    }

    @Data
    public static class Address {
        private String line1;
        private String area;
        private String city;
        private String pin;
        private String state;
        private String country;
    }

    @Data
    public static class Referrer {
        private String type;
        private String name;
        private String phone;
        private String email;
        private String mainComplaint;
    }

    @Data
    public static class Notes {
        private Boolean needsAttention;
        private String text;
    }
}
