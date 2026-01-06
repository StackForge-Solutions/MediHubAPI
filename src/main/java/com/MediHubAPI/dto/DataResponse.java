package com.MediHubAPI.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataResponse<T> {
    private T data;
}
