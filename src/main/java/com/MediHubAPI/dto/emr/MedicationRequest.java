package com.MediHubAPI.dto.emr;

import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationRequest {

    private Long medicineId;
    private String form;
    private String mode;

    private String medicineName;
    private String composition;

    private Boolean inStock;
    private Integer stockQty;

    private Integer m;
    private Integer a;
    private Integer n;

    private Boolean sosEnabled;
    private Integer sosCount;
    private String sosUnit;

    private LocalDate startDate;

    private Boolean lifelong;
    private Integer duration;
    private String durationUnit;

    private String periodicity;
}
