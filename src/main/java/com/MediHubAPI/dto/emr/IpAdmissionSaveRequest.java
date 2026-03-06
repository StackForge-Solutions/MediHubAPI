package com.MediHubAPI.dto.emr;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpAdmissionSaveRequest {

    @NotNull(message = "visitDate is required")
    private LocalDate visitDate;

    @NotBlank(message = "admissionAdvised is required")
    @Pattern(regexp = "(?i)yes|no|na", message = "admissionAdvised must be one of: yes, no, na")
    private String admissionAdvised;

    @Size(max = 500, message = "remarks can be at most 500 characters")
    private String remarks;

    @Size(max = 128, message = "admissionReason can be at most 128 characters")
    private String admissionReason;

    @Min(value = 1, message = "tentativeStayDays must be at least 1")
    private Integer tentativeStayDays;

    @Size(max = 1000, message = "notes can be at most 1000 characters")
    private String notes;
}
