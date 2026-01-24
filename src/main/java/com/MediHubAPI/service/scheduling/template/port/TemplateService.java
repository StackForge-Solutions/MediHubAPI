package com.MediHubAPI.service.scheduling.template.port;

import org.springframework.data.domain.Pageable;
import com.MediHubAPI.dto.scheduling.template.clone.TemplateCloneRequest;
import com.MediHubAPI.dto.scheduling.template.create.TemplateCreateRequest;
import com.MediHubAPI.dto.scheduling.template.get.TemplateDetailDTO;
import com.MediHubAPI.dto.scheduling.template.list.TemplateSearchResponse;
import com.MediHubAPI.dto.scheduling.template.update.TemplateUpdateRequest;
import com.MediHubAPI.model.enums.TemplateScope;

public interface TemplateService {

    TemplateSearchResponse search(TemplateScope scope, Long doctorId, Long departmentId, Boolean active, String q,
            Pageable pageable);

    TemplateDetailDTO create(TemplateCreateRequest request);

    TemplateDetailDTO getById(Long templateId);

    TemplateDetailDTO update(Long templateId, TemplateUpdateRequest request);

    TemplateDetailDTO cloneTemplate(Long templateId, TemplateCloneRequest request);
}
