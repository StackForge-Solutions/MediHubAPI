package com.MediHubAPI.dto.lab;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTestMasterResponse {
    private List<LabTestMasterItemDto> data;
    private LabTestMasterMetaDto meta;
}
