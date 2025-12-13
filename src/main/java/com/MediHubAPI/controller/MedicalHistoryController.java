package com.MediHubAPI.controller;

import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.MedicalHistoryDTO;
import com.MediHubAPI.service.MedicalHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-history")
@RequiredArgsConstructor
@Slf4j
public class MedicalHistoryController {

    private final MedicalHistoryService medicalHistoryService;

    /** ------------------- Save or Update ------------------- */
    @PostMapping
    public ApiResponse<MedicalHistoryDTO> saveOrUpdateMedicalHistory(
            @RequestParam Long appointmentId,
            @RequestBody MedicalHistoryDTO dto
    ) {
        log.info("API call: saveOrUpdateMedicalHistory appointmentId={}", appointmentId);

        MedicalHistoryDTO saved = medicalHistoryService.saveOrUpdateByAppointment(appointmentId, dto);
        return ApiResponse.success(saved, "/api/medical-history", "Medical History saved/updated successfully");
    }

    /** ------------------- Get Medical History ------------------- */
    @GetMapping
    public ApiResponse<MedicalHistoryDTO> getMedicalHistory(
            @RequestParam Long appointmentId
    ) {
        log.info("API call: getMedicalHistory appointmentId={}", appointmentId);

        MedicalHistoryDTO history = medicalHistoryService.getByAppointmentId(appointmentId);
        return ApiResponse.success(history, "/api/medical-history", "Medical History fetched successfully");
    }
}
