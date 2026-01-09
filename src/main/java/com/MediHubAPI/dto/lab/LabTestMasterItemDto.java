package com.MediHubAPI.dto.lab;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTestMasterItemDto {
    private String code;
    private String name;
    private Double amount;
    private Boolean taxable;
    private Integer tatHours;
    private String sampleType;      // optional
    private Boolean active;
    private String updatedAtISO;    // optional (null if not available)
}
