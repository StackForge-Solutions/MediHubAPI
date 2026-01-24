package com.MediHubAPI.dto.scheduling.session.bootstrap;


public record DoctorLiteDTO(
        Long id,
        String displayName,
        String specialization,
        boolean enabled
) {}
