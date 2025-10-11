package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.VisitSummaryDTO;
import com.MediHubAPI.model.VisitSummary;
import com.MediHubAPI.service.VisitSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
    /**
     * Fetch Visit Summary
     * Either by visitSummaryId (path variable) OR by patientId / doctorId / appointmentId (query params)
     */
    @GetMapping
    public ApiResponse<?> getVisitSummary(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long appointmentId
    ) {
        log.info("API call: getVisitSummary id={}, patientId={}, doctorId={}, appointmentId={}",
                id, patientId, doctorId, appointmentId);

        // Fetch by visit summary ID
        if (id != null) {
            VisitSummaryDTO visitSummaryDTO = visitSummaryService.getVisitSummaryById(id);
            return ApiResponse.success(visitSummaryDTO, "/api/visit-summary?id=" + id,
                    "Visit Summary fetched successfully");
        }

        // Search by patientId / doctorId / appointmentId
        List<VisitSummaryDTO> results = visitSummaryService.searchVisitSummaries(patientId, doctorId, appointmentId);
        return ApiResponse.success(results, "/api/visit-summary",
                "Visit Summaries fetched successfully");
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
