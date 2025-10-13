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
    /**
     * ------------------- Create or Update VisitSummary -------------------
     * If an existing record is found for doctorId + patientId + appointmentId,
     * it will be updated. Otherwise, a new record will be created.
     */
    @PostMapping
    public ApiResponse<VisitSummaryDTO> saveOrUpdateVisitSummary(
            @RequestParam Long doctorId,
            @RequestParam Long patientId,
            @RequestParam Long appointmentId,
            @RequestBody VisitSummary visitSummary
    ) {
        log.info("API call: saveOrUpdateVisitSummary doctorId={}, patientId={}, appointmentId={}", doctorId, patientId, appointmentId);

        VisitSummaryDTO saved = visitSummaryService.saveOrUpdateVisitSummary(doctorId, patientId, appointmentId, visitSummary);

        String message = (visitSummary.getId() != null)
                ? "Visit Summary updated successfully"
                : "Visit Summary saved successfully";

        return ApiResponse.success(saved, "/api/visit-summary", message);
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

}