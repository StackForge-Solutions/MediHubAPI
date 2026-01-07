package com.MediHubAPI.service.lab;

import com.MediHubAPI.dto.lab.LabTestSearchDataDto;

public interface LabTestSearchService {
    LabTestSearchDataDto search(String q, Integer limit);
}
