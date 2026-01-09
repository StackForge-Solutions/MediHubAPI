package com.MediHubAPI.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataEnvelope<T> {
    private T data;
    private ApiMeta meta;
}
