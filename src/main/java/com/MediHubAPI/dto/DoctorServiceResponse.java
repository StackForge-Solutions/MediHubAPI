package com.MediHubAPI.dto;

import com.MediHubAPI.model.enums.ServiceStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorServiceResponse {
    private Long id;
    private Long doctorId;
    private String name;
    private BigDecimal price;
    private ServiceStatus status;
    private String code;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
