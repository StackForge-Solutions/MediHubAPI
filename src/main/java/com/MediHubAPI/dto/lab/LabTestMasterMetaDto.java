package com.MediHubAPI.dto.lab;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabTestMasterMetaDto {
    private Integer count;
    private Integer limit;
    private Integer offset;
    private String traceId;
    private String timestamp;
}
