package com.MediHubAPI.service.scheduling.session.port;



import com.MediHubAPI.dto.scheduling.session.bootstrap.DepartmentLiteDTO;

import java.util.List;

public interface DepartmentDirectoryPort {
    List<DepartmentLiteDTO> listDepartments();
}
