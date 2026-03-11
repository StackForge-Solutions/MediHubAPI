package com.MediHubAPI.dto;

import com.MediHubAPI.model.ERole;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class UserUpdateDto {
    private String firstName;
    private String lastName;
    private String mobileNumber;

    @NotEmpty(message = "at least one role is required")
    private Set<ERole> roles;

    @NotNull(message = "enabled is required")
    private Boolean enabled;

    private Long specializationId;
}
