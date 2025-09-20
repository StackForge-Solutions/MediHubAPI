package com.MediHubAPI.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientResponseDto {
    private Long id;
    private String hospitalId;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String email;
    private String specializationName; // from Specialization entity
    private String consultingDoctorName;
    private String photoContentType;
    private boolean photoPresent;
}
