package com.MediHubAPI.controller.scheduling.template;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.MediHubAPI.dto.scheduling.template.clone.TemplateCloneRequest;
import com.MediHubAPI.dto.scheduling.template.create.TemplateCreateRequest;
import com.MediHubAPI.dto.scheduling.template.get.TemplateDetailDTO;
import com.MediHubAPI.dto.scheduling.template.list.TemplateSearchResponse;
import com.MediHubAPI.dto.scheduling.template.update.TemplateUpdateRequest;
import com.MediHubAPI.model.enums.TemplateScope;
import com.MediHubAPI.service.scheduling.template.port.TemplateService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scheduling/templates")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public TemplateSearchResponse search(
            @RequestParam(name = "scope", required = false) TemplateScope scope,
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "departmentId", required = false) Long departmentId,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "q", required = false) String q,
            Pageable pageable
    ) {
        return templateService.search(scope, doctorId, departmentId, active, q, pageable);
    }


    @PostMapping
    public TemplateDetailDTO create(@Valid @RequestBody TemplateCreateRequest request) {
        return templateService.create(request);
    }


    @GetMapping("/{templateId}")
    public TemplateDetailDTO getById(@PathVariable Long templateId) {
        return templateService.getById(templateId);
    }


    @PutMapping("/{templateId}")
    public TemplateDetailDTO update(@PathVariable Long templateId,
            @Valid @RequestBody TemplateUpdateRequest request) {
        return templateService.update(templateId, request);
    }


    @PostMapping("/{templateId}/clone")
    public TemplateDetailDTO cloneTemplate(@PathVariable Long templateId,
            @Valid @RequestBody TemplateCloneRequest request) {
        return templateService.cloneTemplate(templateId, request);
    }
}
