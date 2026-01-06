package com.MediHubAPI.controller.emr;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateRequest;
import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateResponse;
import com.MediHubAPI.service.emr.PrescriptionTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import com.MediHubAPI.dto.emr.template.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emr/prescription-templates")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionTemplateController {

    private final PrescriptionTemplateService prescriptionTemplateService;

    @PostMapping
    public DataResponse<PrescriptionTemplateCreateResponse> createTemplate(
            @Valid @RequestBody PrescriptionTemplateCreateRequest req
    ) {
        log.info("API call: createTemplate name={}, language={}", req.getName(), req.getLanguage());
        PrescriptionTemplateCreateResponse data = prescriptionTemplateService.create(req);
        return new DataResponse<>(data);
    }

    /** 6) List templates (modal) */
    @GetMapping
    public DataResponse<PrescriptionTemplateListDataDto> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        log.info("API call: list prescription templates query={}, limit={}", query, limit);
        return new DataResponse<>(prescriptionTemplateService.list(query, limit));
    }

    /** 7) Get template details (apply) */
    @GetMapping("/{id}")
    public DataResponse<PrescriptionTemplateDetailsDto> getById(@PathVariable("id") String id) {
        log.info("API call: get prescription template id={}", id);
        return new DataResponse<>(prescriptionTemplateService.getById(id));
    }

    /** 8) Update template */
    @PutMapping("/{id}")
    public DataResponse<PrescriptionTemplateListItemDto> update(
            @PathVariable("id") String id,
            @Valid @RequestBody PrescriptionTemplateUpdateRequest req
    ) {
        log.info("API call: update prescription template id={}, name={}", id, req.getName());
        return new DataResponse<>(prescriptionTemplateService.update(id, req));
    }

    /** 9) Delete template (204) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") String id) {
        log.info("API call: delete prescription template id={}", id);
        prescriptionTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
