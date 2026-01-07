package com.MediHubAPI.dto.emr.importprev;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PrescriptionPayloadDto {
    private String language;
    private Boolean followUpEnabled;
    private Integer followUpDuration;
    private String followUpUnit;
    private LocalDate followUpDate;
    private Boolean sendFollowUpEmail;
    private String adviceText;

    private List<MedicationDto> medications;
    private List<TestDto> tests;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MedicationDto {
        private Long id;              // prescription_medications.id (optional)
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

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TestDto {
        private Long id;          // master test id (mdm_pathology_tests.id) if available
        private String name;
        private Double price;
        private Integer tat;
        private Integer quantity;
        private String notes;
        private Boolean isCustom;
    }
}
