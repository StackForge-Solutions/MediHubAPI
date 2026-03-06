package com.MediHubAPI.controller.emr;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.MediHubAPI.dto.ApiResponse;
import com.MediHubAPI.dto.PrescribedTestDTO;
import com.MediHubAPI.service.PrescribedTestService;

@RestController
@RequestMapping("/api/prescribed-tests")
@RequiredArgsConstructor
@Slf4j
public class PrescribedTestController {

    private final PrescribedTestService prescribedTestService;

    /**
     * ------------------- Upsert Prescribed Tests -------------------
     */
    @PostMapping
    public ApiResponse<List<PrescribedTestDTO>> saveOrUpdatePrescribedTests(
            @RequestParam Long appointmentId,
            @RequestBody List<PrescribedTestDTO> testDTOs
    ) {
        log.info("API call: saveOrUpdatePrescribedTests appointmentId={}, testsCount={}", appointmentId,
                testDTOs.size());
        List<PrescribedTestDTO> saved = prescribedTestService.saveOrUpdateTests(appointmentId, testDTOs);
        return ApiResponse.success(saved, "/api/prescribed-tests", "Prescribed Tests saved/updated successfully");
    }

    /**
     * ------------------- Get Prescribed Tests by Appointment ID -------------------
     */
    @GetMapping
    public ApiResponse<List<PrescribedTestDTO>> getPrescribedTests(
            @RequestParam Long appointmentId
    ) {
        log.info("API call: getPrescribedTests appointmentId={}", appointmentId);
        List<PrescribedTestDTO> tests = prescribedTestService.getTestsByAppointmentId(appointmentId);
        return ApiResponse.success(tests, "/api/prescribed-tests", "Prescribed Tests fetched successfully");
    }
}
