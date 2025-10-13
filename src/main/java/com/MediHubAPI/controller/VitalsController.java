package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.VitalsDTO;
import com.MediHubAPI.service.VitalsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vitals")
@RequiredArgsConstructor
@Slf4j
public class VitalsController {

    private final VitalsService vitalsService;

    /** ------------------- Create/Update Vitals ------------------- */
    @PostMapping
    public ApiResponse<VitalsDTO> saveOrUpdateVitals(
            @RequestParam Long appointmentId,
            @RequestBody VitalsDTO dto
    ) {
        log.info("API call: saveOrUpdateVitals appointmentId={}", appointmentId);

        VitalsDTO saved = vitalsService.saveOrUpdateVitals(appointmentId, dto);
        return ApiResponse.success(saved, "/api/vitals", "Vitals saved/updated successfully");
    }
    /** ------------------- Get Vitals by Appointment ID ------------------- */
    @GetMapping
    public ApiResponse<VitalsDTO> getVitals(
            @RequestParam Long appointmentId
    ) {
        log.info("API call: getVitals appointmentId={}", appointmentId);

        VitalsDTO vitals = vitalsService.getVitalsByAppointmentId(appointmentId);
        return ApiResponse.success(vitals, "/api/vitals", "Vitals fetched successfully");
    }

}
