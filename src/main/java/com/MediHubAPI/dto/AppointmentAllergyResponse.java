package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentAllergyResponse {
    private Long appointmentId;
    private Long visitSummaryId;
    private String visitDate;
    private Integer allergyTemplateId;
    private String allergyTemplateName;
    private String allergyCategory;
    private String language;
    private String allergiesText;
}
