package com.MediHubAPI.dto.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentListResponse {
    private int status;
    private List<AppointmentApiDto> data;
    private String message;
}
