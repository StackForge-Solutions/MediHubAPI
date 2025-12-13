package com.MediHubAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalHistoryDTO {

    private Long id;

    private Object personalHistory;
    private Object renalHistory;
    private Object diabetesHistory;
    private Object pastHistory;
    private Object otherHistories;
}
