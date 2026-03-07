package com.MediHubAPI.controller.emr;

import com.MediHubAPI.dto.AllergyTemplateDto;
import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.AppointmentAllergyRequest;
import com.MediHubAPI.dto.AppointmentAllergyResponse;
import com.MediHubAPI.service.AllergyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emr")
@RequiredArgsConstructor
@Slf4j
public class AllergyController {

    private final AllergyService allergyService;

    @GetMapping("/allergy-templates")
    public ApiResponse<List<AllergyTemplateDto>> listTemplates() {
        log.info("API call: list allergy templates");
        List<AllergyTemplateDto> templates = allergyService.listTemplates();
        return ApiResponse.success(templates, "/api/emr/allergy-templates", "Allergy templates fetched");
    }

    @GetMapping("/appointments/{appointmentId}/allergies")
    public ApiResponse<AppointmentAllergyResponse> fetchAllergies(
            @PathVariable Long appointmentId
    ) {
        log.info("API call: fetchAllergies appointmentId={}", appointmentId);
        AppointmentAllergyResponse response = allergyService.getByAppointmentId(appointmentId);
        return ApiResponse.success(response,
                "/api/emr/appointments/" + appointmentId + "/allergies",
                "Allergies fetched successfully");
    }

    @PostMapping("/appointments/{appointmentId}/allergies")
    public ApiResponse<AppointmentAllergyResponse> saveOrUpdateAllergies(
            @PathVariable Long appointmentId,
            @RequestBody AppointmentAllergyRequest request
    ) {
        log.info("API call: saveOrUpdateAllergies appointmentId={}, templateId={}",
                appointmentId, request != null ? request.getAllergyTemplateId() : null);

        AppointmentAllergyResponse response = allergyService.saveOrUpdate(appointmentId, request);

        return ApiResponse.success(response,
                "/api/emr/appointments/" + appointmentId + "/allergies",
                "Allergies saved/updated successfully");
    }
}
