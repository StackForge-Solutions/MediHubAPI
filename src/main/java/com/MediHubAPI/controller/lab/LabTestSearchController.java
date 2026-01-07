package com.MediHubAPI.controller.lab;

import com.MediHubAPI.dto.DataResponse;
import com.MediHubAPI.dto.lab.LabTestSearchDataDto;
import com.MediHubAPI.service.lab.LabTestSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lab/tests")
@RequiredArgsConstructor
@Slf4j
public class LabTestSearchController {

    private final LabTestSearchService labTestSearchService;

    /** ✅ GET /api/lab/tests/search?q=cbc&limit=10 */
    @GetMapping("/search")
    public DataResponse<LabTestSearchDataDto> search(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        log.info("API call: lab tests search q={}, limit={}", q, limit);
        return new DataResponse<>(labTestSearchService.search(q, limit));
    }
}
