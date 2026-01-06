package com.MediHubAPI.service.emr;

import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateRequest;
import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateResponse;

public interface PrescriptionTemplateService {
    PrescriptionTemplateCreateResponse create(PrescriptionTemplateCreateRequest req);
}
