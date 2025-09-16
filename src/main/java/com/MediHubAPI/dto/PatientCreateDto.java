package com.MediHubAPI.dto;

import jakarta.validation.constraints.*;

import com.MediHubAPI.model.enums.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientCreateDto {
    // Demographics
    @NotBlank(message = "First name is mandatory")
    private String firstName;

    @NotBlank(message = "Last name is mandatory")
    private String lastName;

    @NotBlank(message = "Password is mandatory")
    private String password;

    @NotBlank(message = "Mobile number is mandatory")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;

    private String alternateContact;
    private String landlineNumber;


    @NotNull(message = "Date of birth is mandatory")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;



    @NotNull(message = "Sex is mandatory")
    private Sex sex;
    private MaritalStatus maritalStatus;

    @Email private String email;

    private GovtIdType govtIdType;
    private String govtIdNumber;

    private String otherHospitalIds;
    private String motherTongue;

    // Referrer
    private ReferrerType referrerType;
    private String referrerName;
    private String referrerNumber;
    @Email private String referrerEmail;

    // Doctor FK + Department + Complaint
    @NotNull(message = "Consulting doctor ID is mandatory")
    private Long consultingDoctorId;// FK to User (doctor)  //validation requires

    //  @NotNull private Department department;
    @NotNull(message = "Specialization ID is mandatory")
    private Long specializationId;  // reference Specialization//validation requires


    @NotBlank private String mainComplaint;

    // Additional details
    private BloodGroup bloodGroup;
    private String fathersName;
    private String mothersName;
    private String spousesName;

    private Education education;
    private Occupation occupation;
    private Religion religion;

    private Double birthWeightValue;
    private String birthWeightUnit; // kg/g/lb






}