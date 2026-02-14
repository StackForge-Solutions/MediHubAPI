package com.MediHubAPI.dto.lab;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTestDetailsDto {
    private Long patientId;
    private String name;
    private String hospitalId;
    private String ageSex;
    private String dob;
    private String billDate;
    private String billNo;
    private List<TestItem> tests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestItem {
        private String code;
        private String name;
        private Boolean authorized;
        private String sampleStatus;
        private String result;
        private String unit;
        private String reference;
        private Boolean outOfRange;
    }
}
