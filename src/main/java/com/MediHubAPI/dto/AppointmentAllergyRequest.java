package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentAllergyRequest {
    private Long appointmentId;      // optional duplicate of path for safety
    private String visitDate;        // optional override/confirmation
    private Integer allergyTemplateId;
    private String allergiesText;
}
