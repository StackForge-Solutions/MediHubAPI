package com.MediHubAPI.service;

import com.MediHubAPI.dto.VisitRowDTO;
import com.MediHubAPI.repository.AppointmentQueryRepository;
import com.MediHubAPI.web.request.VisitFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitQueryService {

    private final AppointmentQueryRepository repository;


    public Page<VisitRowDTO> search(VisitFilter filter, Pageable pageable) {

        LocalDate start;
        LocalDate end;

        // period handling
        if ("RANGE".equalsIgnoreCase(filter.getPeriod())) {
            start = filter.getFrom();
            end   = filter.getTo();
        } else {
            start = filter.getDate();
            end   = filter.getDate();
        }

        String q = normalizeQ(filter.getQ());

        log.info(
                "Visit search | start={} end={} q={} page={} size={}",
                start, end, q,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        return repository.findVisitRows(
                start,
                end,
                filter.getDoctorId(),
                filter.getPatientId(),
                filter.getHasLabTests(),
                q,
                pageable
        );
    }
    private String normalizeQ(String q) {
        return (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
    }



}
