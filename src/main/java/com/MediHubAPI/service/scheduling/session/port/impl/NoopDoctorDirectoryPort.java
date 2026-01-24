package com.MediHubAPI.service.scheduling.session.port.impl;

import com.MediHubAPI.dto.scheduling.session.bootstrap.DoctorLiteDTO;
import com.MediHubAPI.service.scheduling.session.port.DoctorDirectoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnMissingBean(DoctorDirectoryPort.class)
public class NoopDoctorDirectoryPort implements DoctorDirectoryPort {

    @Override
    public List<DoctorLiteDTO> listDoctors() {
        return List.of();
    }
}
