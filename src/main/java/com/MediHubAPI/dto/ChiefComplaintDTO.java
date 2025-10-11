package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChiefComplaintDTO {
    private String complaint;
    private int years;
    private int months;
    private int weeks;
    private int days;
    private int sinceYear;
    private String bodyPart;
}
