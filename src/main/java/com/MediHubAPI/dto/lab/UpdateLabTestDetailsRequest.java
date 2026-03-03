package com.MediHubAPI.dto.lab;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLabTestDetailsRequest {

    @NotNull
    private Long patientId;

    @NotBlank
    private String billNo;

    private String billDate;
    private String room;
    private String date;

    @NotEmpty
    private List<TestItem> tests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestItem {
        @NotBlank
        private String code;
        private String name;
        private Boolean authorized;
        private String sampleStatus;
        private String result;
        private String unit;
        private String reference;
        private Object outOfRange;
    }
}
