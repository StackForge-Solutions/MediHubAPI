package com.MediHubAPI.service.lab;

import com.MediHubAPI.dto.lab.LabTestMasterResponse;

public interface LabTestMasterService {
    LabTestMasterResponse getMaster(Boolean active, String q, Integer limit, Integer offset, String sort);
}
