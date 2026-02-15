package com.MediHubAPI.dto;

import com.MediHubAPI.model.ERole;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class UserCreateDto {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private Set<ERole> roles; // already done 

    private LocalDate activationDate;

    private Long specializationId;  // or use String specializationName


}