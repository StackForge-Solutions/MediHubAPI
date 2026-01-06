package com.MediHubAPI.service.emr;

import com.MediHubAPI.dto.emr.template.*;

public interface PrescriptionTemplateService {
    PrescriptionTemplateCreateResponse create(PrescriptionTemplateCreateRequest req);

    PrescriptionTemplateListDataDto list(String query, Integer limit);

    PrescriptionTemplateDetailsDto getById(String tplId);

    PrescriptionTemplateListItemDto update(String tplId, PrescriptionTemplateUpdateRequest req);

    void delete(String tplId);
}
