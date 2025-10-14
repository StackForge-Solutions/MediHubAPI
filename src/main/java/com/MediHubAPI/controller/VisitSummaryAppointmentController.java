package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.VisitSummaryDTO;
import com.MediHubAPI.service.VisitSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visit-summary")
@RequiredArgsConstructor
@Slf4j
public class VisitSummaryAppointmentController {

    private final VisitSummaryService visitSummaryService;

    /** ------------------- Upsert VisitSummary by appointmentId ------------------- */
    @PostMapping("/upsert-by-appointment")
    public ApiResponse<VisitSummaryDTO> upsertByAppointment(
            @RequestParam Long appointmentId
    ) {
        log.info("API call: upsertByAppointment appointmentId={}", appointmentId);

        VisitSummaryDTO dto = visitSummaryService.upsertByAppointment(appointmentId);

        return ApiResponse.success(dto, "/api/visit-summary/upsert-by-appointment",
                "Visit Summary ready for appointmentId=" + appointmentId);
    }

    /** ------------------- Fetch VisitSummary by appointmentId ------------------- */
    @GetMapping("/by-appointment")
    public ApiResponse<VisitSummaryDTO> getByAppointment(
            @RequestParam Long appointmentId
    ) {
        log.info("API call: getByAppointment appointmentId={}", appointmentId);

        VisitSummaryDTO dto = visitSummaryService.getByAppointmentId(appointmentId);

        return ApiResponse.success(dto, "/api/visit-summary/by-appointment",
                "Visit Summary fetched successfully for appointmentId=" + appointmentId);
    }
}
