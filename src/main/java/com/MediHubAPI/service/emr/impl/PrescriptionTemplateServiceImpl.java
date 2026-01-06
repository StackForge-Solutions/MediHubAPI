package com.MediHubAPI.service.emr.impl;


import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateRequest;
import com.MediHubAPI.dto.emr.template.PrescriptionTemplateCreateResponse;
import com.MediHubAPI.exception.ConflictException;
import com.MediHubAPI.model.emr.PrescriptionTemplate;
import com.MediHubAPI.repository.emr.PrescriptionTemplateRepository;
import com.MediHubAPI.service.emr.PrescriptionTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionTemplateServiceImpl implements PrescriptionTemplateService {

    private final PrescriptionTemplateRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PrescriptionTemplateCreateResponse create(PrescriptionTemplateCreateRequest req) {

        String name = req.getName().trim();

        repository.findByNameIgnoreCase(name).ifPresent(existing -> {
            throw new ConflictException(
                    "TEMPLATE_NAME_EXISTS",
                    "A prescription template named '" + name + "' already exists."
            );
        });

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(req.getPayload());
        } catch (Exception e) {
            throw new IllegalArgumentException("payload is not valid JSON");
        }

        PrescriptionTemplate entity = PrescriptionTemplate.builder()
                .name(name)
                .language(req.getLanguage().trim())
                .payload(payloadJson)
                .isActive(true)
                .build();

        PrescriptionTemplate saved = repository.save(entity);

        return PrescriptionTemplateCreateResponse.builder()
                .id("tpl_" + saved.getId())
                .name(saved.getName())
                .language(saved.getLanguage())
                .createdAt(saved.getCreatedAt().toInstant(ZoneOffset.UTC))
                .updatedAt(saved.getUpdatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }
}

