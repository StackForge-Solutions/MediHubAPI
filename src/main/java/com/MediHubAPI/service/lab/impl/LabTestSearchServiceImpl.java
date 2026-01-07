package com.MediHubAPI.service.lab.impl;

import com.MediHubAPI.dto.lab.LabTestSearchDataDto;
import com.MediHubAPI.dto.lab.LabTestSearchItemDto;
import com.MediHubAPI.exception.lab.InvalidLabTestQueryException;
import com.MediHubAPI.model.mdm.PathologyTestMaster;
import com.MediHubAPI.repository.MasterTestRepository;
import com.MediHubAPI.service.lab.LabTestSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabTestSearchServiceImpl implements LabTestSearchService {

    private final MasterTestRepository repo;

    @Override
    @Transactional(readOnly = true)
    public LabTestSearchDataDto search(String q, Integer limit) {

        if (q == null || q.trim().length() < 2) {
            throw new InvalidLabTestQueryException();
        }

        int safeLimit = (limit == null || limit <= 0) ? 10 : Math.min(limit, 50);
        PageRequest page = PageRequest.of(0, safeLimit);

        String query = q.trim();

        List<PathologyTestMaster> rows =
                repo.findByIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(query, page);

        List<LabTestSearchItemDto> items = rows.stream()
                .map(t -> LabTestSearchItemDto.builder()
                        .id(t.getId())
                        .name(t.getName())
                        .price(t.getPrice())
                        .tat(t.getTat())
                        .build())
                .collect(Collectors.toList());

        log.info("Lab test search: q={}, limit={}, returned={}", query, safeLimit, items.size());

        return LabTestSearchDataDto.builder()
                .items(items)
                .count(items.size())
                .limit(safeLimit)
                .query(query)
                .build();
    }
}
