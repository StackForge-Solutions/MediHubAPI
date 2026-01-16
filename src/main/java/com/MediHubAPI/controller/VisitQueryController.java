package com.MediHubAPI.controller;

import com.MediHubAPI.dto.VisitRowDTO;
import com.MediHubAPI.service.VisitQueryService;
import com.MediHubAPI.web.request.VisitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitQueryController {

    private final VisitQueryService service;

    /**
     * GET /api/visits?period=DAY&date=2025-09-26&page=0&size=20
     * GET /api/visits?period=WEEK&date=2025-09-26
     * GET /api/visits?period=MONTH&date=2025-09-01
     * GET /api/visits?period=RANGE&from=2025-09-01&to=2025-09-30&doctorId=12
     * GET /api/visits?sort=appointmentDate,desc;slotTime,desc
     */
    @GetMapping
    public Page<VisitRowDTO> getVisits(
            VisitFilter filter,
            Pageable pageable
    ) {
        return service.search(filter, pageable);
    }
}


