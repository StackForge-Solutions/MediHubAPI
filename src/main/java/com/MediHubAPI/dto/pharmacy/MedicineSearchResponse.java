package com.MediHubAPI.dto.pharmacy;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineSearchResponse {
    private List<MedicineSearchItemDto> data;
    private MedicineSearchMeta meta;
}
