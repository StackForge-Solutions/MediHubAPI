package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VitalsDTO {
    private Long id;
    private Double height;
    private Double weight;
    private Double bmi;
    private Double waist;
    private Integer bpSystolic;
    private Integer bpDiastolic;
    private Integer pulse;
    private Double hc;
    private Double temperature;
    private Integer respiratory;
}
