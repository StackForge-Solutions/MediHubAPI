package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.ChiefComplaintDTO;
import com.MediHubAPI.service.ChiefComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chief-complaints")
@RequiredArgsConstructor
@Slf4j
public class ChiefComplaintController {

    private final ChiefComplaintService chiefComplaintService;

    /** ------------------- UPSERT by appointmentId ------------------- */
    @PostMapping
    public ApiResponse<List<ChiefComplaintDTO>> upsertChiefComplaints(
            @RequestParam Long appointmentId,
            @RequestBody List<ChiefComplaintDTO> complaints
    ) {
        log.info("API call: upsertChiefComplaints appointmentId={}, count={}", appointmentId, complaints.size());
        List<ChiefComplaintDTO> saved = chiefComplaintService.upsertByAppointmentId(appointmentId, complaints);
        return ApiResponse.success(saved, "/api/chief-complaints", "Chief complaints upserted successfully");
    }

    /** ------------------- GET by appointmentId ------------------- */
    @GetMapping
    public ApiResponse<List<ChiefComplaintDTO>> getChiefComplaints(
            @RequestParam Long appointmentId
    ) {
        log.info("API call: getChiefComplaints appointmentId={}", appointmentId);
        List<ChiefComplaintDTO> list = chiefComplaintService.getByAppointmentId(appointmentId);
        return ApiResponse.success(list, "/api/chief-complaints", "Chief complaints fetched successfully");
    }
}
