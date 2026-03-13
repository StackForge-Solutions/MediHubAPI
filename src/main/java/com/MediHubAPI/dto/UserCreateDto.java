package com.MediHubAPI.dto;

import com.MediHubAPI.model.ERole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserCreateDto {
    @NotBlank(message = "username is required")
    private String username;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "password is required")
    private String password;

    private String firstName;
    private String lastName;
    private String mobileNumber;

    @NotEmpty(message = "at least one role is required")
    private Set<ERole> roles;

    private LocalDate activationDate;
    private Long specializationId;
}
