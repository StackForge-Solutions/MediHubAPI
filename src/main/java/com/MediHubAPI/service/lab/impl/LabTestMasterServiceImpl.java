package com.MediHubAPI.service.lab.impl;

import com.MediHubAPI.dto.lab.*;
import com.MediHubAPI.repository.lab.LabTestMasterRepository;
import com.MediHubAPI.repository.projection.LabTestMasterRowProjection;
import com.MediHubAPI.service.lab.LabTestMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabTestMasterServiceImpl implements LabTestMasterService {

    private final LabTestMasterRepository repository;

    @Override
    @Transactional(readOnly = true)
    public LabTestMasterResponse getMaster(Boolean active, String q, Integer limit, Integer offset, String sort) {

        int safeLimit = (limit == null || limit <= 0) ? 50 : Math.min(limit, 200);
        int safeOffset = (offset == null || offset < 0) ? 0 : offset;

        // active: default true
        Boolean activeVal = (active == null) ? Boolean.TRUE : active;
        Integer activeInt = (activeVal == null) ? null : (activeVal ? 1 : 0);

        String safeQ = (q == null) ? null : q.trim();
        String safeSort = (sort == null || sort.trim().isEmpty()) ? "name:asc" : sort.trim();

        List<LabTestMasterRowProjection> rows =
                repository.fetchMaster(activeInt, safeQ, safeLimit, safeOffset, safeSort);

        List<LabTestMasterItemDto> data = rows.stream()
                .map(r -> LabTestMasterItemDto.builder()
                        .code(r.getCode())
                        .name(r.getName())
                        .amount(r.getAmount() == null ? 0.0 : r.getAmount())
                        .taxable(r.getTaxable() != null && r.getTaxable() == 1)
                        .tatHours(r.getTatHours())
                        .sampleType(r.getSampleType())
                        .active(r.getActive() == null || r.getActive() == 1)
                        .updatedAtISO(r.getUpdatedAtISO()) // will be null currently
                        .build())
                .collect(Collectors.toList());

        LabTestMasterMetaDto meta = LabTestMasterMetaDto.builder()
                .count(data.size())
                .limit(safeLimit)
                .offset(safeOffset)
                .traceId(null) // controller will fill (optional)
                .timestamp(Instant.now().toString())
                .build();

        log.info("Lab master fetched: active={}, q={}, limit={}, offset={}, returned={}",
                activeVal, safeQ, safeLimit, safeOffset, data.size());

        return LabTestMasterResponse.builder()
                .data(data)
                .meta(meta)
                .build();
    }
}
