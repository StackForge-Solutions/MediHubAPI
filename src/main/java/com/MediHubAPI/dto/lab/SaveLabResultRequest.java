package com.MediHubAPI.dto.lab;

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
public class SaveLabResultRequest {
    @NotNull
    private Long invoiceId;

    @NotEmpty
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @NotNull
        private Long invoiceItemId;
        private String sampleStatus; // pending/completed/noshow/reserved
        private String result;
        private String unit;
        private String reference;
        private Boolean outOfRange;
        private Boolean authorized;
    }
}
