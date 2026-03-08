package com.MediHubAPI.dto.emr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryPrintDto {
    private String date;
    private String complaint;
    private VitalsSummary vitals;
    private String allergies;
    private List<String> medicalHistory;
    private List<String> organInvolvement;
    private List<String> renalHistory;
    private List<String> personalHistory;
    private List<String> diagnosis;
    private DoctorSummary doctor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VitalsSummary {
        private Double height;
        private Double weight;
        private Double bmi;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DoctorSummary {
        private String name;
        private String degree;
        private String regn;
    }
}
