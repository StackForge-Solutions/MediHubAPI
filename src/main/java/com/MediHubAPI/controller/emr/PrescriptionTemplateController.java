package com.MediHubAPI.controller.emr;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateRequest;
import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateResponse;
import com.MediHubAPI.service.emr.PrescriptionTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
