package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.VisitSummaryDTO;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.service.VisitSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/visit-summary")
@RequiredArgsConstructor
@Slf4j
public class VisitSummaryController {

    private final VisitSummaryService visitSummaryService;

    /** ------------------- Create VisitSummary ------------------- */
    @PostMapping
    public ApiResponse<VisitSummaryDTO> saveVisitSummary(
            @RequestParam Long doctorId,
            @RequestParam Long patientId,
            @RequestBody VisitSummary visitSummary
    ) {
        log.info("API call: saveVisitSummary doctorId={}, patientId={}", doctorId, patientId);

        VisitSummaryDTO saved = visitSummaryService.saveVisitSummary(doctorId, patientId, visitSummary);

        return ApiResponse.created(saved, "/api/visit-summary", "Visit Summary saved successfully");
    }

    /** ------------------- Get VisitSummary by ID ------------------- */
    @GetMapping("/{id}")
    public ApiResponse<VisitSummaryDTO> getVisitSummaryById(@PathVariable Long id) {
        log.info("API call: getVisitSummaryById id={}", id);

        VisitSummaryDTO visitSummaryDTO = visitSummaryService.getVisitSummaryById(id);

        return ApiResponse.success(visitSummaryDTO, "/api/visit-summary/" + id, "Visit Summary fetched successfully");
    }

    /** ------------------- Update VisitSummary ------------------- */
    @PutMapping("/{id}")
    public ApiResponse<VisitSummaryDTO> updateVisitSummary(
            @PathVariable Long id,
            @RequestBody VisitSummary visitSummary
    ) {
        log.info("API call: updateVisitSummary id={}", id);

        VisitSummaryDTO updated = visitSummaryService.updateVisitSummary(id, visitSummary);

        return ApiResponse.success(updated, "/api/visit-summary/" + id, "Visit Summary updated successfully");
    }
}
