package com.MediHubAPI.service.scheduling.session.port.impl;


import com.MediHubAPI.dto.scheduling.session.bootstrap.DepartmentLiteDTO;
import com.MediHubAPI.service.scheduling.session.port.DepartmentDirectoryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;


import java.util.List;

@Component
@ConditionalOnMissingBean(DepartmentDirectoryPort.class)
public class NoopDepartmentDirectoryPort implements DepartmentDirectoryPort {
    @Override
    public List<DepartmentLiteDTO> listDepartments() {
        // TODO: adapt to Specialization.department or Department entity
        return List.of();
    }
}
