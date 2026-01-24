package com.MediHubAPI.service.scheduling.session.port;


import com.MediHubAPI.dto.scheduling.session.bootstrap.DoctorLiteDTO;

import java.util.List;

public interface DoctorDirectoryPort {
    List<DoctorLiteDTO> listDoctors();
}
