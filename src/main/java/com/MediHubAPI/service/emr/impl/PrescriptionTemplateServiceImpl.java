package com.MediHubAPI.service.emr.impl;


import com.MediHubAPI.dto.emr.template.*;
import com.MediHubAPI.exception.ConflictException;
import com.MediHubAPI.exception.emr.PrescriptionTemplateNameExistsException;
import com.MediHubAPI.exception.emr.PrescriptionTemplateNotFoundException;
import com.MediHubAPI.model.emr.PrescriptionTemplate;
import com.MediHubAPI.repository.emr.PrescriptionTemplateRepository;
import com.MediHubAPI.service.emr.PrescriptionTemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

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


    @Override
    @Transactional(readOnly = true)
    public PrescriptionTemplateListDataDto list(String query, Integer limit) {

        int safeLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        PageRequest page = PageRequest.of(0, safeLimit);

        List<PrescriptionTemplate> rows;
        String q = (query == null) ? null : query.trim();

        if (q == null || q.isEmpty()) {
            rows = repository.findByIsActiveTrueOrderByUpdatedAtDesc(page);
        } else {
            rows = repository.findByIsActiveTrueAndNameContainingIgnoreCaseOrderByUpdatedAtDesc(q, page);
        }

        List<PrescriptionTemplateListItemDto> items = rows.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());

        return PrescriptionTemplateListDataDto.builder()
                .items(items)
                .count(items.size())
                .limit(safeLimit)
                .query(q)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionTemplateDetailsDto getById(String tplId) {

        Long id = parseTplId(tplId);

        PrescriptionTemplate entity = repository.findById(id)
                .filter(PrescriptionTemplate::getIsActive)
                .orElseThrow(() -> new PrescriptionTemplateNotFoundException(tplId));

        JsonNode payloadNode;
        try {
            payloadNode = objectMapper.readTree(entity.getPayload());
        } catch (Exception e) {
            // If payload got corrupted in DB, still fail loudly
            throw new IllegalArgumentException("Template payload JSON is invalid in DB for id: " + tplId);
        }

        return PrescriptionTemplateDetailsDto.builder()
                .id("tpl_" + entity.getId())
                .name(entity.getName())
                .language(entity.getLanguage())
                .createdAt(entity.getCreatedAt().toInstant(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().toInstant(ZoneOffset.UTC))
                .payload(payloadNode)
                .build();
    }

    @Override
    public PrescriptionTemplateListItemDto update(String tplId, PrescriptionTemplateUpdateRequest req) {
        return null;
    }

    @Override
    @Transactional
    public void delete(String tplId) {
        Long id = parseTplId(tplId);

        PrescriptionTemplate entity = repository.findById(id)
                .filter(PrescriptionTemplate::getIsActive)
                .orElseThrow(() -> new PrescriptionTemplateNotFoundException(tplId));

        // soft delete (recommended)
        entity.setIsActive(false);
        repository.save(entity);
    }

    // ---------------- helpers ----------------

    private PrescriptionTemplateListItemDto toListItem(PrescriptionTemplate e) {
        return PrescriptionTemplateListItemDto.builder()
                .id("tpl_" + e.getId())
                .name(e.getName())
                .language(e.getLanguage())
                .createdAt(e.getCreatedAt().toInstant(ZoneOffset.UTC))
                .updatedAt(e.getUpdatedAt().toInstant(ZoneOffset.UTC))
                .build();
    }

    private Long parseTplId(String tplId) {
        if (tplId == null || tplId.trim().isEmpty()) {
            throw new IllegalArgumentException("id is required");
        }
        String raw = tplId.trim();
        if (raw.startsWith("tpl_")) raw = raw.substring(4);

        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid template id: " + tplId);
        }
    }
}


