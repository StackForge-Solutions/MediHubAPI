package com.MediHubAPI.dto.scheduling.session.bootstrap;


public record DoctorLiteDTO(
        Long id,
        String name,
        String speciality,
        boolean enabled,
        long departmentId
) {}
